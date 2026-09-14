package com.tcc.pjb.backend.service.competencia;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.procedural.ProceduralCanonicalResolver;
import com.tcc.pjb.backend.core.procedural.ProceduralCanonicalResolver.CanonicalContext;
import com.tcc.pjb.backend.core.util.Hashes;
import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.model.dto.competencia.CompetenceResolveRequest;
import com.tcc.pjb.backend.model.dto.competencia.DynamicCompetenceDistributionRequest;
import com.tcc.pjb.backend.model.dto.competencia.DynamicCompetenceDistributionResponse;
import com.tcc.pjb.backend.model.dto.competencia.DynamicCompetenceRedistributionResponse;
import com.tcc.pjb.backend.model.entity.Jurisdicao;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.competencia.Comarca;
import com.tcc.pjb.backend.model.entity.competencia.ModoOperacaoUnidadeJudiciaria;
import com.tcc.pjb.backend.model.entity.competencia.ProcessoDistribuicaoCompetencia;
import com.tcc.pjb.backend.model.entity.competencia.StatusDistribuicaoCompetencia;
import com.tcc.pjb.backend.model.entity.competencia.UnidadeJudiciariaCompetencia;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.repository.ProcessoDistribuicaoCompetenciaRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.UnidadeJudiciariaCompetenciaRepository;
import com.tcc.pjb.backend.service.outbox.OutboxPublisher;
import com.tcc.pjb.backend.tribunal.distribuicao.ConfiguracaoDistribuicaoVaraService;
import com.tcc.pjb.backend.platform.runtime.PjbTransactionalBudget;

@Service
public class MapaCompetenciaDinamicoEngine {

    private record CachedUnits(List<UnidadeJudiciariaCompetencia> units, Instant expiresAt) {
    }

    public static final String EVT_COMPETENCIA_DISTRIBUIDA = "pjb.competencia.distribuida";
    public static final String EVT_COMPETENCIA_REVISAO = "pjb.competencia.revisao_humana";
    public static final String EVT_COMPETENCIA_REDISTRIBUICAO = "pjb.competencia.redistribuicao.sugerida";
    private static final Duration UNIT_CACHE_TTL = Duration.ofSeconds(20);

    private final UnidadeJudiciariaCompetenciaRepository unidadeRepository;
    private final ProcessoDistribuicaoCompetenciaRepository distribuicaoRepository;
    private final ProcessoRepository processoRepository;
    private final AuditLedgerService auditLedgerService;
    private final OutboxPublisher outboxPublisher;
    private final CompetenceResolverService competenceResolverService;
    private final ConfiguracaoDistribuicaoVaraService configuracaoDistribuicaoVaraService;
    private final ProceduralCanonicalResolver proceduralCanonicalResolver;
    private final AtomicReference<CachedUnits> unitsCache = new AtomicReference<>();

    public MapaCompetenciaDinamicoEngine(UnidadeJudiciariaCompetenciaRepository unidadeRepository,
                                         ProcessoDistribuicaoCompetenciaRepository distribuicaoRepository,
                                         ProcessoRepository processoRepository,
                                         AuditLedgerService auditLedgerService,
                                         OutboxPublisher outboxPublisher,
                                         CompetenceResolverService competenceResolverService,
                                         ConfiguracaoDistribuicaoVaraService configuracaoDistribuicaoVaraService,
                                         ProceduralCanonicalResolver proceduralCanonicalResolver) {
        this.unidadeRepository = Objects.requireNonNull(unidadeRepository);
        this.distribuicaoRepository = Objects.requireNonNull(distribuicaoRepository);
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.auditLedgerService = Objects.requireNonNull(auditLedgerService);
        this.outboxPublisher = Objects.requireNonNull(outboxPublisher);
        this.competenceResolverService = Objects.requireNonNull(competenceResolverService);
        this.configuracaoDistribuicaoVaraService = Objects.requireNonNull(configuracaoDistribuicaoVaraService);
        this.proceduralCanonicalResolver = Objects.requireNonNull(proceduralCanonicalResolver);
    }

    @Transactional(readOnly = true)
    public Optional<DynamicCompetenceDistributionResponse> distribuir(DynamicCompetenceDistributionRequest request) {
        return distribuirInterno(sanitizar(request), false);
    }

    @Transactional
    public Optional<DynamicCompetenceDistributionResponse> distribuirERegistrar(DynamicCompetenceDistributionRequest request) {
        return distribuirInterno(sanitizar(request), true);
    }

    @Transactional
    public Optional<DynamicCompetenceDistributionResponse> registrarDistribuicaoInicial(Processo processo) {
        if (processo == null) {
            return Optional.empty();
        }
        DynamicRequest completaRequest = buildFromProcesso(processo);
        Optional<DynamicCompetenceDistributionResponse> completa = distribuirInterno(completaRequest, false);
        if (completa.isPresent() && completa.get().distribuicaoAutomatica()) {
            return distribuirInterno(completaRequest, true);
        }
        DynamicRequest fallbackRequest = buildFallbackFromProcesso(processo);
        Optional<DynamicCompetenceDistributionResponse> fallback = distribuirInterno(fallbackRequest, false);
        if (fallback.isPresent() && fallback.get().distribuicaoAutomatica()) {
            return distribuirInterno(fallbackRequest, true);
        }
        return registrarDistribuicaoMinima(processo);
    }

    @PjbTransactionalBudget(operation = "competencia.mapa-dinamico.analisar-redistribuicao", maxMillis = 8000)
    @Transactional
    public DynamicCompetenceRedistributionResponse analisarRedistribuicao(double limiarCongestionamento) {
        double limiar = Math.max(0.5d, Math.min(0.99d, limiarCongestionamento));
        List<UnidadeJudiciariaCompetencia> unidades = carregarSnapshotUnidades();
        List<DynamicCompetenceRedistributionResponse.Proposal> propostas = new ArrayList<>();
        for (UnidadeJudiciariaCompetencia origem : unidades) {
            if (origem.getIndiceCongestionamento() == null || origem.getIndiceCongestionamento().doubleValue() < limiar) {
                continue;
            }
            List<UnidadeJudiciariaCompetencia> destinos = unidades.stream()
                    .filter(item -> !Objects.equals(item.getId(), origem.getId()))
                    .filter(UnidadeJudiciariaCompetencia::estaAptaParaDistribuicao)
                    .filter(item -> Objects.equals(tribunalSigla(item), tribunalSigla(origem)))
                    .filter(item -> mesmaComarcaResolvida(item, origem))
                    .filter(item -> ufCompativelOuDesconhecida(comarcaUf(item), comarcaUf(origem)))
                    .filter(item -> item.getTipoVara() == origem.getTipoVara())
                    .filter(item -> Objects.equals(item.getTipoJustica(), origem.getTipoJustica()))
                    .filter(item -> Objects.equals(item.getRamoDireito(), origem.getRamoDireito()))
                    .sorted(Comparator
                            .comparingDouble(UnidadeJudiciariaCompetencia::disponibilidadeOperacional).reversed()
                            .thenComparing(UnidadeJudiciariaCompetencia::getPrioridadeEstrategica, Comparator.reverseOrder()))
                    .limit(3)
                    .toList();
            if (destinos.isEmpty()) {
                continue;
            }
            UnidadeJudiciariaCompetencia destino = destinos.get(0);
            int quantidadeSugerida = Math.max(1,
                    (int) Math.round((origem.getIndiceCongestionamento().doubleValue() - 0.70d)
                            * Math.max(1, origem.getProcessosAtivos())
                            * 0.30d));
            double compatibilidade = round2((destino.disponibilidadeOperacional() * 60.0d)
                    + (destino.getPrioridadeEstrategica() * 0.20d)
                    + ((1.0d - origem.getIndiceCongestionamento().doubleValue()) * -10.0d)
                    + 30.0d);
            propostas.add(new DynamicCompetenceRedistributionResponse.Proposal(
                    origem.getCodigo(),
                    destino.getCodigo(),
                    quantidadeSugerida,
                    "Redistribuicao sugerida para equalizar carga, preservar capacidade de contingencia e reduzir tempo de fila.",
                    compatibilidade
            ));
        }
        if (!propostas.isEmpty()) {
            outboxPublisher.enqueue(
                    "competencia:redistribuicao",
                    EVT_COMPETENCIA_REDISTRIBUICAO,
                    Map.of("quantidadePropostas", propostas.size(), "geradoEm", Instant.now().toString()),
                    Map.of("source", "mapa_competencia"),
                    "competenciaRedistribuicao:" + Hashes.sha256Hex(String.valueOf(propostas.size()) + Instant.now().truncatedTo(ChronoUnit.MINUTES)),
                    "CompetenciaRedistribuicao",
                    String.valueOf(propostas.size())
            );
        }
        return new DynamicCompetenceRedistributionResponse(List.copyOf(propostas));
    }

    private List<UnidadeJudiciariaCompetencia> carregarSnapshotUnidades() {
        CachedUnits cache = unitsCache.get();
        Instant now = Instant.now();
        if (cache != null && !cache.units().isEmpty() && cache.expiresAt() != null && cache.expiresAt().isAfter(now)) {
            return cache.units();
        }
        synchronized (unitsCache) {
            cache = unitsCache.get();
            now = Instant.now();
            if (cache != null && !cache.units().isEmpty() && cache.expiresAt() != null && cache.expiresAt().isAfter(now)) {
                return cache.units();
            }
            List<UnidadeJudiciariaCompetencia> loaded = List.copyOf(unidadeRepository.findAll());
            // As entidades sobrevivem no cache além da transação/sessão que as carregou. As três
            // @ElementCollection(EAGER) precisam ser inicializadas aqui, dentro da sessão de carga —
            // caso contrário uma leitura posterior a partir do cache (sessão já fechada) estoura
            // LazyInitializationException, mesmo a coleção sendo declarada EAGER na entidade.
            loaded.forEach(unidade -> {
                org.hibernate.Hibernate.initialize(unidade.getEspecialidades());
                org.hibernate.Hibernate.initialize(unidade.getClassesTpu());
                org.hibernate.Hibernate.initialize(unidade.getAssuntosTpu());
            });
            unitsCache.set(loaded.isEmpty() ? null : new CachedUnits(loaded, now.plus(UNIT_CACHE_TTL)));
            return loaded;
        }
    }

    private Optional<DynamicCompetenceDistributionResponse> distribuirInterno(DynamicRequest request, boolean persistir) {
        request = enriquecerInferencia(request);
        List<UnidadeJudiciariaCompetencia> unidades = carregarSnapshotUnidades();
        if (unidades.isEmpty()) {
            return Optional.empty();
        }

        Map<Long, Long> distribuicoesRecentes = carregarDistribuicoesRecentes(unidades);
        double mediaDistribuicoes = distribuicoesRecentes.isEmpty()
                ? 0.0d
                : distribuicoesRecentes.values().stream().mapToLong(Long::longValue).average().orElse(0.0d);

        List<Candidate> candidatos = new ArrayList<>();
        for (UnidadeJudiciariaCompetencia unidade : unidades) {
            Candidate candidate = avaliar(unidade, request, distribuicoesRecentes.getOrDefault(unidade.getId(), 0L), mediaDistribuicoes);
            if (candidate != null && candidate.scoreFinal() > 0.0d) {
                candidatos.add(candidate);
            }
        }

        candidatos.sort(Comparator
                .comparingDouble(Candidate::scoreFinal).reversed()
                .thenComparing(Comparator.comparingDouble(Candidate::scoreDisponibilidade).reversed())
                .thenComparing(candidate -> candidate.unidade().getPrioridadeEstrategica(), Comparator.reverseOrder())
                .thenComparing(candidate -> candidate.unidade().getCodigo()));

        if (candidatos.isEmpty()) {
            return Optional.empty();
        }

        Candidate melhor = candidatos.get(0);
        double margem = candidatos.size() > 1 ? round2(melhor.scoreFinal() - candidatos.get(1).scoreFinal()) : 100.0d;
        List<String> fatoresRevisao = new ArrayList<>(melhor.fatoresRevisao());
        if (!melhor.unidade().isPermiteDistribuicaoAutomatica()) {
            fatoresRevisao.add("Unidade configurada para revisao humana obrigatoria");
        }
        if (margem < 3.0d) {
            fatoresRevisao.add("Diferenca pequena entre a melhor unidade e a alternativa seguinte");
        }
        if (melhor.scoreFinal() < 78.0d) {
            fatoresRevisao.add("Confianca operacional inferior ao limiar de automacao");
        }
        if (melhor.scoreTerritorial() < 18.0d) {
            fatoresRevisao.add("Aderencia territorial parcial requer validacao humana");
        }
        fatoresRevisao = List.copyOf(new LinkedHashSet<>(fatoresRevisao));

        boolean distribuicaoAutomatica = fatoresRevisao.isEmpty() && melhor.unidade().isPermiteDistribuicaoAutomatica();
        List<DynamicCompetenceDistributionResponse.AlternativeUnit> alternativas = candidatos.stream()
                .skip(1)
                .limit(3)
                .map(candidate -> new DynamicCompetenceDistributionResponse.AlternativeUnit(
                        candidate.unidade().getCodigo(),
                        tribunalSigla(candidate.unidade()),
                        comarcaNome(candidate.unidade()),
                        comarcaUf(candidate.unidade()),
                        candidate.unidade().getTipoVara().name(),
                        round2(candidate.scoreFinal())
                ))
                .toList();

        String requestId = UUID.randomUUID().toString();
        DynamicCompetenceDistributionResponse response = new DynamicCompetenceDistributionResponse(
                requestId,
                Instant.now(),
                request.nupn(),
                melhor.unidade().getCodigo(),
                tribunalSigla(melhor.unidade()),
                comarcaNome(melhor.unidade()),
                comarcaUf(melhor.unidade()),
                melhor.unidade().getTipoVara().name(),
                round2(melhor.scoreFinal()),
                distribuicaoAutomatica,
                melhor.motivacao(),
                List.copyOf(melhor.alertas()),
                fatoresRevisao,
                alternativas,
                new DynamicCompetenceDistributionResponse.ScoreBreakdown(
                        round2(melhor.scoreTerritorial()),
                        round2(melhor.scoreEspecialidade()),
                        round2(melhor.scoreDisponibilidade()),
                        round2(melhor.scoreEquilibrio()),
                        round2(melhor.scoreAderenciaNormativa())
                )
        );

        if (persistir) {
            persistirDistribuicao(request, melhor, response, fatoresRevisao, distribuicaoAutomatica);
        }

        return Optional.of(response);
    }

    private Optional<DynamicCompetenceDistributionResponse> registrarDistribuicaoMinima(Processo processo) {
        DynamicRequest request = buildFallbackFromProcesso(processo);
        List<UnidadeJudiciariaCompetencia> unidades = carregarSnapshotUnidades();
        if (unidades.isEmpty()) {
            return Optional.empty();
        }
        Optional<UnidadeJudiciariaCompetencia> selecionada = selecionarUnidadeMinima(unidades, request, true, true)
                .or(() -> selecionarUnidadeMinima(unidades, request, false, true))
                .or(() -> selecionarUnidadeMinima(unidades, request, false, false));
        if (selecionada.isEmpty()) {
            return Optional.empty();
        }
        UnidadeJudiciariaCompetencia unidade = selecionada.get();
        List<String> fatoresTerritoriais = new ArrayList<>();
        List<String> fatoresNormativos = new ArrayList<>();
        double territorial = scoreTerritorial(unidade, request, fatoresTerritoriais);
        double especialidade = Math.max(9.0d, scoreEspecialidade(unidade, request, new ArrayList<>()));
        double disponibilidade = round2(unidade.disponibilidadeOperacional() * 15.0d);
        double equilibrio = round2(Math.min(10.0d, 7.0d + (unidade.getPrioridadeEstrategica() / 100.0d)));
        double aderenciaNormativa = scoreAderenciaNormativa(unidade, request, fatoresNormativos);
        double scoreFinal = round2(territorial + especialidade + disponibilidade + equilibrio + aderenciaNormativa);
        boolean distribuicaoAutomatica = unidade.isPermiteDistribuicaoAutomatica();
        List<String> fatoresRevisao = distribuicaoAutomatica ? List.of() : List.of("Unidade configurada para revisao humana obrigatoria");
        String motivacao = construirMotivacao(unidade, scoreFinal, territorial, especialidade, disponibilidade, equilibrio, aderenciaNormativa);
        Candidate candidate = new Candidate(
                unidade,
                scoreFinal,
                territorial,
                especialidade,
                disponibilidade,
                equilibrio,
                aderenciaNormativa,
                List.of(),
                fatoresRevisao,
                motivacao
        );
        DynamicCompetenceDistributionResponse response = new DynamicCompetenceDistributionResponse(
                UUID.randomUUID().toString(),
                Instant.now(),
                request.nupn(),
                unidade.getCodigo(),
                tribunalSigla(unidade),
                comarcaNome(unidade),
                comarcaUf(unidade),
                unidade.getTipoVara().name(),
                scoreFinal,
                distribuicaoAutomatica,
                motivacao,
                List.of(),
                fatoresRevisao,
                List.of(),
                new DynamicCompetenceDistributionResponse.ScoreBreakdown(
                        territorial,
                        especialidade,
                        disponibilidade,
                        equilibrio,
                        aderenciaNormativa
                )
        );
        persistirDistribuicao(request, candidate, response, fatoresRevisao, distribuicaoAutomatica);
        return Optional.of(response);
    }

    private Optional<UnidadeJudiciariaCompetencia> selecionarUnidadeMinima(List<UnidadeJudiciariaCompetencia> unidades,
                                                                           DynamicRequest request,
                                                                           boolean exigirRamo,
                                                                           boolean exigirValor) {
        return unidades.stream()
                .filter(UnidadeJudiciariaCompetencia::estaAptaParaDistribuicao)
                .filter(unidade -> suportaTipoJustica(unidade, request.tipoJustica()))
                .filter(unidade -> !exigirRamo || suportaRamoDireito(unidade, request.ramoDireito()))
                .filter(unidade -> !exigirValor || unidade.suportaValorCausa(request.valorCausa()))
                .filter(unidade -> aderenciaTerritorialMinima(unidade, request) > 0)
                .sorted(Comparator
                        .comparingInt((UnidadeJudiciariaCompetencia unidade) -> aderenciaTerritorialMinima(unidade, request)).reversed()
                        .thenComparing(Comparator.comparingDouble(UnidadeJudiciariaCompetencia::disponibilidadeOperacional).reversed())
                        .thenComparing(UnidadeJudiciariaCompetencia::getPrioridadeEstrategica, Comparator.reverseOrder())
                        .thenComparing(unidade -> Objects.toString(unidade.getCodigo(), "")))
                .findFirst();
    }

    private Candidate avaliar(UnidadeJudiciariaCompetencia unidade,
                              DynamicRequest request,
                              long distribuicoesRecentes,
                              double mediaDistribuicoes) {
        if (unidade == null || !unidade.estaAptaParaDistribuicao()) {
            return null;
        }
        if (!suportaTipoJustica(unidade, request.tipoJustica())) {
            return null;
        }
        if (!suportaRamoDireito(unidade, request.ramoDireito())) {
            return null;
        }
        if (!unidade.suportaValorCausa(request.valorCausa())) {
            return null;
        }
        if (request.requerJuizadoEspecial() && !unidade.isPermiteJuizadoEspecial()) {
            return null;
        }
        if (request.casoUrgente() && !unidade.isPermiteUrgencia()) {
            return null;
        }
        if (request.preferenciaDigital() && unidade.getModoOperacao() == ModoOperacaoUnidadeJudiciaria.PRESENCIAL) {
            return null;
        }

        ConfiguracaoDistribuicaoVaraService.ResultadoConsulta restricaoDistribuicao = configuracaoDistribuicaoVaraService.consultar(
                unidade.getCodigo(),
                new ConfiguracaoDistribuicaoVaraService.FiltroDistribuicao(
                        request.materiaPrincipal(),
                        null,
                        request.classeTpu(),
                        request.assuntoTpu(),
                        request.valorCausa(),
                        request.requerJuizadoEspecial(),
                        true,
                        request.ufReu() != null ? request.ufReu() : request.ufAutor(),
                        request.comarcaReu() != null ? request.comarcaReu() : request.comarcaAutor(),
                        request.casoUrgente(),
                        request.preferenciaDigital(),
                        true
                )
        );
        if (!restricaoDistribuicao.aceita()) {
            return null;
        }

        List<String> alertas = new ArrayList<>();
        List<String> fatoresRevisao = new ArrayList<>();

        double scoreTerritorial = scoreTerritorial(unidade, request, fatoresRevisao);
        double scoreEspecialidade = scoreEspecialidade(unidade, request, fatoresRevisao);
        double scoreDisponibilidade = round2(unidade.disponibilidadeOperacional() * 15.0d);
        double scoreEquilibrio = scoreEquilibrio(unidade, distribuicoesRecentes, mediaDistribuicoes);
        double scoreAderenciaNormativa = scoreAderenciaNormativa(unidade, request, fatoresRevisao);

        if (unidade.getIndiceCongestionamento() != null && unidade.getIndiceCongestionamento().doubleValue() > 0.80d) {
            alertas.add("Unidade com congestionamento elevado");
        }
        if (unidade.getProcessosAtivos() >= unidade.getCapacidadeMaxima()) {
            alertas.add("Capacidade nominal da unidade atingida");
        }
        if (request.requerVaraEspecializada() && unidade.getEspecialidades().isEmpty()) {
            fatoresRevisao.add("Pedido indica especializacao, mas a unidade opera como fila geral");
        }
        if (request.preferenciaDigital() && unidade.getEnderecoDigital() == null) {
            fatoresRevisao.add("Preferencia digital indicada sem endereco digital cadastrado na unidade");
        }

        double scoreFinal = round2(scoreTerritorial + scoreEspecialidade + scoreDisponibilidade + scoreEquilibrio + scoreAderenciaNormativa);
        if (scoreFinal <= 0.0d) {
            return null;
        }

        String motivacao = construirMotivacao(unidade, scoreFinal, scoreTerritorial, scoreEspecialidade, scoreDisponibilidade, scoreEquilibrio, scoreAderenciaNormativa);
        return new Candidate(unidade,
                scoreFinal,
                scoreTerritorial,
                scoreEspecialidade,
                scoreDisponibilidade,
                scoreEquilibrio,
                scoreAderenciaNormativa,
                List.copyOf(new LinkedHashSet<>(alertas)),
                List.copyOf(new LinkedHashSet<>(fatoresRevisao)),
                motivacao);
    }

    private void persistirDistribuicao(DynamicRequest request,
                                       Candidate melhor,
                                       DynamicCompetenceDistributionResponse response,
                                       List<String> fatoresRevisao,
                                       boolean distribuicaoAutomatica) {
        Processo processo = request.processoId() == null ? null : processoRepository.getReferenceById(request.processoId());
        UnidadeJudiciariaCompetencia unidadePersistente = unidadePersistente(melhor.unidade());
        String requestHash = calcularRequestHash(request);
        if (distribuicaoRepository.findTopByRequestHashOrderByIdDesc(requestHash).isPresent()) {
            return;
        }
        ProcessoDistribuicaoCompetencia distribuicao = new ProcessoDistribuicaoCompetencia(
                processo,
                unidadePersistente,
                request.nupn(),
                response.scoreFinal(),
                response.scoreBreakdown().territorial(),
                response.scoreBreakdown().especialidade(),
                response.scoreBreakdown().disponibilidade(),
                response.scoreBreakdown().equilibrio(),
                response.scoreBreakdown().aderenciaNormativa(),
                distribuicaoAutomatica,
                distribuicaoAutomatica ? StatusDistribuicaoCompetencia.APLICADA : StatusDistribuicaoCompetencia.PENDENTE_REVISAO,
                response.motivacao(),
                String.join(" | ", response.alertas()),
                String.join(" | ", fatoresRevisao),
                requestHash
        );
        try {
            distribuicaoRepository.saveAndFlush(distribuicao);
        } catch (DataIntegrityViolationException ex) {
            if (distribuicaoRepository.findTopByRequestHashOrderByIdDesc(requestHash).isPresent()) {
                return;
            }
            throw ex;
        }
        if (processo != null) {
            aplicarSnapshotDistribuicaoAoProcesso(processo, unidadePersistente, distribuicaoAutomatica, response, fatoresRevisao);
            processoRepository.save(processo);
        }
        if (distribuicaoAutomatica) {
            Long unidadeId = melhor.unidade().getId();
            if (unidadeId != null) {
                if (unidadeRepository.registrarDistribuicaoAplicada(unidadeId, Instant.now()) > 0) {
                    unitsCache.set(null);
                }
            }
        }
        String aggregateId = processo != null && processo.getId() != null
                ? String.valueOf(processo.getId())
                : (request.nupn() != null ? request.nupn() : melhor.unidade().getCodigo());
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("nupn", request.nupn());
        payload.put("processoId", request.processoId());
        payload.put("unidadeCodigo", melhor.unidade().getCodigo());
        payload.put("tribunalCodigo", tribunalSigla(melhor.unidade()));
        payload.put("scoreFinal", response.scoreFinal());
        payload.put("distribuicaoAutomatica", distribuicaoAutomatica);
        payload.put("alertas", response.alertas());
        payload.put("fatoresRevisaoHumana", fatoresRevisao);
        payload.put("geradoEm", response.generatedAt().toString());
        outboxPublisher.enqueue(
                "competencia:" + aggregateId,
                distribuicaoAutomatica ? EVT_COMPETENCIA_DISTRIBUIDA : EVT_COMPETENCIA_REVISAO,
                payload,
                Map.of("source", "mapa_competencia"),
                "competenciaDistribuicao:" + requestHash,
                "CompetenciaDistribuicao",
                aggregateId
        );
        auditLedgerService.appendSafely(
                distribuicaoAutomatica ? "COMPETENCIA_DISTRIBUIDA" : "COMPETENCIA_REVISAO_HUMANA",
                "COMPETENCIA_DISTRIBUICAO",
                aggregateId,
                requestHash,
                melhor.unidade().getCodigo() + "|score=" + response.scoreFinal()
        );
    }

    private UnidadeJudiciariaCompetencia unidadePersistente(UnidadeJudiciariaCompetencia unidade) {
        if (unidade == null || unidade.getId() == null) {
            return unidade;
        }
        return unidadeRepository.getReferenceById(unidade.getId());
    }

    private void aplicarSnapshotDistribuicaoAoProcesso(Processo processo,
                                                   UnidadeJudiciariaCompetencia unidade,
                                                   boolean distribuicaoAutomatica,
                                                   DynamicCompetenceDistributionResponse response,
                                                   List<String> fatoresRevisao) {
        if (processo == null || unidade == null) {
            return;
        }
        String tribunalCodigo = tribunalCodigoSnapshot(unidade, response, processo.getTribunalCodigoRoteado());
        processo.setTribunalCodigoRoteado(tribunalCodigo);
        processo.setUnidadeJudiciariaCodigo(unidade.getCodigo());
        processo.setTribunal(firstNonBlank(tribunalCodigo, processo.getTribunal()));
        processo.setComarca(firstNonBlank(comarcaNome(unidade), processo.getComarca()));
        processo.setUf(firstNonBlank(comarcaUf(unidade), processo.getUf()));
        if (unidade.getComarcaEntidade() != null) {
            processo.setComarcaEntidade(unidade.getComarcaEntidade());
        }
        processo.setVara(resolveVaraSnapshot(unidade, processo.getVara()));
        if (distribuicaoAutomatica) {
            processo.setDataDistribuicao(java.time.LocalDateTime.now());
        }
        processo.setPreProtocoloStatus(distribuicaoAutomatica ? "DISTRIBUICAO_AUTOMATICA_APLICADA" : "DISTRIBUICAO_EM_REVISAO_HUMANA");
        if (response != null) {
            processo.setRoutingConfidence(BigDecimal.valueOf(response.scoreFinal()).setScale(4, RoundingMode.HALF_UP));
        }
        if (response != null && response.alertas() != null && !response.alertas().isEmpty()) {
            processo.setConnectorSubmissionMessage(joinDistinct(processo.getConnectorSubmissionMessage(), response.alertas()));
        }
        if (fatoresRevisao != null && !fatoresRevisao.isEmpty()) {
            processo.setConnectorSyncMessage(joinDistinct(processo.getConnectorSyncMessage(), fatoresRevisao));
        }
    }

    private String tribunalCodigoSnapshot(UnidadeJudiciariaCompetencia unidade,
                                          DynamicCompetenceDistributionResponse response,
                                          String atual) {
        String direto = firstNonBlank(
                unidade != null ? tribunalSigla(unidade) : null,
                response != null ? response.tribunalCodigo() : null
        );
        if (direto != null) {
            return direto;
        }
        String unidadeCodigo = normalizeText(unidade != null ? unidade.getCodigo() : null);
        if (unidadeCodigo != null) {
            int separador = unidadeCodigo.indexOf('_');
            if (separador > 0) {
                return unidadeCodigo.substring(0, separador);
            }
        }
        return atual;
    }

    private String resolveVaraSnapshot(UnidadeJudiciariaCompetencia unidade, String atual) {
        if (unidade == null) {
            return atual;
        }
        String codigo = normalizeText(unidade.getCodigo());
        String tipo = unidade.getTipoVara() != null ? unidade.getTipoVara().name() : null;
        return firstNonBlank(codigo, tipo, atual);
    }

    private String joinDistinct(String current, List<String> extra) {
        LinkedHashSet<String> itens = new LinkedHashSet<>();
        if (current != null && !current.isBlank()) {
            for (String part : current.split("\\s*\\|\\s*")) {
                String normalized = normalizeText(part);
                if (normalized != null) {
                    itens.add(normalized);
                }
            }
        }
        if (extra != null) {
            for (String item : extra) {
                String normalized = normalizeText(item);
                if (normalized != null) {
                    itens.add(normalized);
                }
            }
        }
        return itens.isEmpty() ? null : String.join(" | ", itens);
    }

    private DynamicRequest buildFromProcesso(Processo processo) {
        Jurisdicao jurisdicao = processo.getJurisdicao();
        String ufJuridicao = jurisdicao != null ? jurisdicao.getUf() : null;
        String comarcaJurisdicao = jurisdicao != null ? jurisdicao.getCidade() : null;
        String ufBase = firstNonBlank(processo.getUf(), ufJuridicao);
        String comarcaBase = firstNonBlank(processo.getComarca(), comarcaJurisdicao, processo.getVara());
        CanonicalContext canonicalContext = proceduralCanonicalResolver.resolve(Map.ofEntries(
                Map.entry("rito", processo.getRito() != null ? processo.getRito().name() : ""),
                Map.entry("ramoDireito", processo.getRamoDireito() != null ? processo.getRamoDireito().name() : ""),
                Map.entry("classeTpu", Objects.toString(processo.getClasseProcessual(), "")),
                Map.entry("classeProcessual", Objects.toString(processo.getClasseProcessual(), "")),
                Map.entry("assunto", Objects.toString(processo.getAssunto(), "")),
                Map.entry("competencia", processo.getTipoJustica() != null ? processo.getTipoJustica().name() : ""),
                Map.entry("uf", Objects.toString(ufBase, "")),
                Map.entry("comarca", Objects.toString(comarcaBase, "")),
                Map.entry("tribunalCodigo", Objects.toString(firstNonBlank(processo.getTribunalCodigoRoteado(), jurisdicao != null ? jurisdicao.getSigla() : null), ""))
        ));
        TipoJustica canonicalTipoJustica = parseTipoJustica(canonicalContext.ramoJusticaNacional());
        RamoDireito canonicalRamo = parseRamo(canonicalContext.ramoDireito());
        String classeTpu = canonicalContext.classeTpuNome() != null ? canonicalContext.classeTpuNome() : processo.getClasseProcessual();
        String assuntoTpu = firstNonBlank(processo.getAssunto(), processo.getObjetoProcessual());
        boolean requerJuizadoEspecial = ritoIndicaJuizado(canonicalContext.rito() != null ? canonicalContext.rito() : processo.getRito());
        boolean requerVaraEspecializada = requiresSpecializedUnit(processo, canonicalContext);
        boolean casoUrgente = requiresUrgentDistribution(processo, canonicalContext);
        boolean preferenciaDigital = prefersDigitalDistribution(processo);
        String materiaPrincipal = firstNonBlank(
                processo.getMateria() != null ? processo.getMateria().name() : null,
                canonicalContext.ramoDireito(),
                processo.getAssunto(),
                processo.getClasseProcessual());
        return sanitizar(new DynamicCompetenceDistributionRequest(
                firstNonBlank(processo.getNumeroUnificado(), processo.getNumeroProcesso()),
                classeTpu,
                assuntoTpu,
                canonicalRamo != null ? canonicalRamo.name() : (processo.getRamoDireito() != null ? processo.getRamoDireito().name() : null),
                processo.getValorCausa(),
                ufBase,
                comarcaBase,
                ufBase,
                comarcaBase,
                requerJuizadoEspecial,
                requerVaraEspecializada,
                materiaPrincipal,
                canonicalTipoJustica != null ? canonicalTipoJustica.name() : (processo.getTipoJustica() != null ? processo.getTipoJustica().name() : null),
                casoUrgente,
                preferenciaDigital,
                processo.getId()
        ));
    }

    private DynamicRequest buildFallbackFromProcesso(Processo processo) {
        Jurisdicao jurisdicao = processo.getJurisdicao();
        String ufBase = firstNonBlank(processo.getUf(), jurisdicao != null ? jurisdicao.getUf() : null);
        String comarcaBase = firstNonBlank(processo.getComarca(), jurisdicao != null ? jurisdicao.getCidade() : null, processo.getVara());
        return new DynamicRequest(
                firstNonBlank(processo.getNumeroUnificado(), processo.getNumeroProcesso()),
                null,
                null,
                processo.getRamoDireito(),
                processo.getValorCausa(),
                ufBase,
                comarcaBase,
                ufBase,
                comarcaBase,
                false,
                false,
                null,
                processo.getTipoJustica(),
                false,
                prefersDigitalDistribution(processo),
                processo.getId()
        );
    }

    private boolean requiresSpecializedUnit(Processo processo, CanonicalContext canonicalContext) {
        if (processo == null) {
            return false;
        }
        if (processo.getMateria() != null) {
            return true;
        }
        if (processo.getRito() != null) {
            if (processo.getRito().isPenal()
                    || processo.getRito().isTrabalhista()
                    || processo.getRito().isEleitoral()
                    || processo.getRito().isMilitar()
                    || processo.getRito().isPrevidenciario()
                    || processo.getRito().isTribFazenda()
                    || processo.getRito().isInfancia()
                    || processo.getRito().isAmbiental()
                    || processo.getRito().isAgrario()
                    || processo.getRito().isEmpresarial()
                    || processo.getRito().isAdministrativo()
                    || processo.getRito().isInternacional()
                    || processo.getRito().isAutocompositivo()) {
                return true;
            }
            String nomeRito = processo.getRito().name();
            if (nomeRito.contains("FAMILIA")
                    || nomeRito.contains("INVENTARIO")
                    || nomeRito.contains("SUCESS")
                    || nomeRito.contains("JURI")
                    || nomeRito.contains("JUIZADO")
                    || nomeRito.contains("FALENCIA")
                    || nomeRito.contains("RECUPERACAO")
                    || nomeRito.contains("AMBIENTAL")
                    || nomeRito.contains("AGRARIO")
                    || nomeRito.contains("IMPROBIDADE")
                    || nomeRito.contains("MANDADO_INJUNCAO")
                    || nomeRito.contains("ADPF")
                    || nomeRito.contains("ADI")) {
                return true;
            }
        }
        String unidade = normalizeUpper(firstNonBlank(processo.getUnidadeJudiciariaCodigo(), processo.getVara()));
        if (unidade != null && (unidade.contains("JUIZADO")
                || unidade.contains("CRIM")
                || unidade.contains("JURI")
                || unidade.contains("FAZENDA")
                || unidade.contains("FAMILIA")
                || unidade.contains("SUCESS")
                || unidade.contains("INFANCIA")
                || unidade.contains("ELEITORAL")
                || unidade.contains("TRABALHO")
                || unidade.contains("FEDERAL")
                || unidade.contains("AMBIENTAL")
                || unidade.contains("AGRAR")
                || unidade.contains("EMPRESARIAL")
                || unidade.contains("FALENCIA")
                || unidade.contains("RECUPERACAO")
                || unidade.contains("ADMINISTRATIVO")
                || unidade.contains("IMPROBIDADE")
                || unidade.contains("CEJUSC")
                || unidade.contains("ARBITRAGEM")
                || unidade.contains("INTERNACIONAL")
                || unidade.contains("CONSTITUCIONAL"))) {
            return true;
        }
        return canonicalContext != null && normalizeUpper(canonicalContext.ramoDireito()) != null && !"CIVIL".equals(normalizeUpper(canonicalContext.ramoDireito()));
    }

    private boolean requiresUrgentDistribution(Processo processo, CanonicalContext canonicalContext) {
        if (processo == null) {
            return false;
        }
        if (containsUrgencySignal(processo.getRoutingRiskLevel()) || containsUrgencySignal(processo.getPreProtocoloStatus()) || containsUrgencySignal(processo.getSubmissionBlueprintStatus()) || containsUrgencySignal(processo.getConnectorSubmissionStatus())) {
            return true;
        }
        if (processo.getRito() != null) {
            return processo.getRito() == com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual.ESPECIAL_HABEAS_CORPUS
                    || processo.getRito() == com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual.PENAL_HABEAS_CORPUS_PREVENTIVO
                    || processo.getRito() == com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual.CIVIL_TUTELA_URGENTE
                    || processo.getRito() == com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual.CIVIL_TUTELA_CAUTELAR_ANTECEDENTE
                    || processo.getRito() == com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual.CIVIL_TUTELA_ANTECIPADA_ANTECEDENTE
                    || processo.getRito() == com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual.ESPECIAL_MANDADO_SEGURANCA
                    || processo.getRito() == com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual.ESPECIAL_MANDADO_SEGURANCA_COLETIVO;
        }
        return canonicalContext != null && containsUrgencySignal(canonicalContext.rito() != null ? canonicalContext.rito().toString() : null);
    }

    private boolean prefersDigitalDistribution(Processo processo) {
        if (processo == null) {
            return false;
        }
        if (normalizeText(processo.getConnectorSystem()) != null || normalizeText(processo.getSubmissionBlueprintStatus()) != null || normalizeText(processo.getSubmissionDryRunStatus()) != null) {
            return true;
        }
        String unidade = normalizeUpper(processo.getUnidadeJudiciariaCodigo());
        return unidade != null && (unidade.contains("DIGITAL") || unidade.contains("VIRTUAL") || unidade.contains("REMOTO"));
    }

    private boolean containsUrgencySignal(String value) {
        String normalized = normalizeUpper(value);
        return normalized != null && (normalized.contains("PLANTAO")
                || normalized.contains("URGENT")
                || normalized.contains("LIMINAR")
                || normalized.contains("TUTELA")
                || normalized.contains("CRITIC"));
    }

    private Map<Long, Long> carregarDistribuicoesRecentes(List<UnidadeJudiciariaCompetencia> unidades) {
        Map<Long, Long> mapa = new HashMap<>();
        Collection<Long> unidadeIds = unidades.stream()
                .map(UnidadeJudiciariaCompetencia::getId)
                .filter(Objects::nonNull)
                .toList();
        if (unidadeIds.isEmpty()) {
            return mapa;
        }
        Instant limite = Instant.now().minus(24, ChronoUnit.HOURS);
        for (Object[] linha : distribuicaoRepository.contarRecentesPorUnidade(unidadeIds, limite)) {
            if (linha == null || linha.length < 2 || linha[0] == null || linha[1] == null) {
                continue;
            }
            mapa.put(((Number) linha[0]).longValue(), ((Number) linha[1]).longValue());
        }
        for (Long unidadeId : unidadeIds) {
            mapa.putIfAbsent(unidadeId, 0L);
        }
        return mapa;
    }

    private double scoreTerritorial(UnidadeJudiciariaCompetencia unidade, DynamicRequest request, List<String> fatoresRevisao) {
        double score = 0.0d;
        if (equalOrBlank(comarcaUf(unidade), request.ufReu())) {
            score = Math.max(score, 18.0d);
            if (equalOrBlank(comarcaNome(unidade), request.comarcaReu())) {
                return round2(30.0d);
            }
        }
        if (equalOrBlank(comarcaUf(unidade), request.ufAutor())) {
            score = Math.max(score, 14.0d);
            if (equalOrBlank(comarcaNome(unidade), request.comarcaAutor())) {
                score = Math.max(score, 22.0d);
            }
        }
        if (score == 0.0d) {
            if (comarcaUf(unidade) == null || comarcaNome(unidade) == null) {
                score = 8.0d;
                fatoresRevisao.add("Catastro territorial incompleto da unidade");
            } else {
                fatoresRevisao.add("Correspondencia territorial nao foi exata");
            }
        }
        return round2(score);
    }

    int aderenciaTerritorialMinima(UnidadeJudiciariaCompetencia unidade, DynamicRequest request) {
        String ufUnidade = normalizeUpper(comarcaUf(unidade));
        String comarcaUnidade = normalizeUpper(comarcaNome(unidade));
        String ufProcesso = normalizeUpper(firstNonBlank(request.ufReu(), request.ufAutor()));
        String comarcaProcesso = normalizeUpper(firstNonBlank(request.comarcaReu(), request.comarcaAutor()));
        int score = 0;
        if (ufProcesso == null || (ufUnidade != null && ufUnidade.equals(ufProcesso))) {
            score += 2;
        } else if (ufUnidade == null) {
            return 1;
        } else {
            return 0;
        }
        if (comarcaProcesso == null || (comarcaUnidade != null && comarcaUnidade.equals(comarcaProcesso))) {
            score += 2;
        }
        return score;
    }

    private double scoreEspecialidade(UnidadeJudiciariaCompetencia unidade, DynamicRequest request, List<String> fatoresRevisao) {
        double score = 0.0d;
        boolean materiaInformada = request.materiaPrincipal() != null && !request.materiaPrincipal().isBlank();
        if (materiaInformada && unidade.suportaMateria(request.materiaPrincipal())) {
            score += unidade.getEspecialidades().isEmpty() ? 10.0d : 16.0d;
        } else if (materiaInformada && request.requerVaraEspecializada()) {
            fatoresRevisao.add("Especialidade principal nao coincide integralmente com a unidade");
        } else if (!materiaInformada && unidade.getEspecialidades().isEmpty()) {
            score += 9.0d;
        }
        if (unidade.suportaClasse(request.classeTpu())) {
            score += 5.0d;
        } else if (request.classeTpu() != null) {
            fatoresRevisao.add("Classe TPU sem aderencia direta no catalogo da unidade");
        }
        if (unidade.suportaAssunto(request.assuntoTpu())) {
            score += 4.0d;
        }
        return round2(Math.min(25.0d, score));
    }

    private double scoreEquilibrio(UnidadeJudiciariaCompetencia unidade, long distribuicoesRecentes, double mediaDistribuicoes) {
        double score = 0.0d;
        if (mediaDistribuicoes <= 0.0d) {
            score += 7.0d;
        } else {
            double razao = distribuicoesRecentes / mediaDistribuicoes;
            double fator = Math.max(0.0d, 1.0d - Math.max(0.0d, razao - 1.0d));
            score += fator * 7.0d;
        }
        score += Math.min(3.0d, unidade.getPrioridadeEstrategica() / 33.0d);
        return round2(Math.min(10.0d, score));
    }

    private double scoreAderenciaNormativa(UnidadeJudiciariaCompetencia unidade, DynamicRequest request, List<String> fatoresRevisao) {
        double score = 0.0d;
        if (request.tipoJustica() != null && unidade.getTipoJustica() == request.tipoJustica()) {
            score += 6.0d;
        } else if (request.tipoJustica() == null || unidade.getTipoJustica() == null) {
            score += 2.0d;
        }
        if (request.ramoDireito() != null && unidade.getRamoDireito() == request.ramoDireito()) {
            score += 5.0d;
        } else if (request.ramoDireito() == null || unidade.getRamoDireito() == null) {
            score += 2.0d;
        }
        if (request.valorCausa() != null && unidade.suportaValorCausa(request.valorCausa())) {
            score += 3.0d;
        }
        if (request.requerJuizadoEspecial() && unidade.isPermiteJuizadoEspecial()) {
            score += 2.0d;
        }
        if (request.casoUrgente() && unidade.isPermiteUrgencia()) {
            score += 2.0d;
        }
        if (request.preferenciaDigital()) {
            if (unidade.getModoOperacao() == ModoOperacaoUnidadeJudiciaria.DIGITAL || unidade.getModoOperacao() == ModoOperacaoUnidadeJudiciaria.HIBRIDA) {
                score += 2.0d;
            } else {
                fatoresRevisao.add("Preferencia digital sem aderencia plena do modo operacional da unidade");
            }
        }
        return round2(Math.min(20.0d, score));
    }

    private boolean suportaTipoJustica(UnidadeJudiciariaCompetencia unidade, TipoJustica tipoJustica) {
        return tipoJustica == null || unidade.getTipoJustica() == null || unidade.getTipoJustica() == tipoJustica;
    }

    private boolean suportaRamoDireito(UnidadeJudiciariaCompetencia unidade, RamoDireito ramoDireito) {
        return ramoDireito == null || unidade.getRamoDireito() == null || unidade.getRamoDireito() == ramoDireito;
    }

    private static String construirMotivacao(UnidadeJudiciariaCompetencia unidade,
                                             double scoreFinal,
                                             double territorial,
                                             double especialidade,
                                             double disponibilidade,
                                             double equilibrio,
                                             double aderenciaNormativa) {
        return "Unidade " + unidade.getCodigo()
                + " escolhida com score=" + scoreFinal
                + ", territorial=" + territorial
                + ", especialidade=" + especialidade
                + ", disponibilidade=" + disponibilidade
                + ", equilibrio=" + equilibrio
                + ", aderenciaNormativa=" + aderenciaNormativa;
    }


    private DynamicRequest enriquecerInferencia(DynamicRequest request) {
        if (request == null) {
            return null;
        }
        CanonicalContext canonicalContext = proceduralCanonicalResolver.resolve(Map.ofEntries(
                Map.entry("classeTpu", Objects.toString(request.classeTpu(), "")),
                Map.entry("assunto", Objects.toString(request.assuntoTpu(), "")),
                Map.entry("ramoDireito", request.ramoDireito() != null ? request.ramoDireito().name() : ""),
                Map.entry("materia", Objects.toString(request.materiaPrincipal(), "")),
                Map.entry("competencia", request.tipoJustica() != null ? request.tipoJustica().name() : ""),
                Map.entry("uf", Objects.toString(request.ufReu() != null ? request.ufReu() : request.ufAutor(), "")),
                Map.entry("comarca", Objects.toString(request.comarcaReu() != null ? request.comarcaReu() : request.comarcaAutor(), ""))
        ));
        TipoJustica canonicalTipoJustica = parseTipoJustica(canonicalContext.ramoJusticaNacional());
        RamoDireito canonicalRamo = parseRamo(canonicalContext.ramoDireito());
        if (request.tipoJustica() != null && request.ramoDireito() != null) {
            return request;
        }
        if (canonicalTipoJustica != null || canonicalRamo != null) {
            return new DynamicRequest(
                    request.nupn(),
                    request.classeTpu(),
                    request.assuntoTpu(),
                    canonicalRamo != null ? canonicalRamo : request.ramoDireito(),
                    request.valorCausa(),
                    request.ufAutor(),
                    request.comarcaAutor(),
                    request.ufReu(),
                    request.comarcaReu(),
                    request.requerJuizadoEspecial() || ritoIndicaJuizado(canonicalContext.rito()),
                    request.requerVaraEspecializada(),
                    request.materiaPrincipal(),
                    canonicalTipoJustica != null ? canonicalTipoJustica : request.tipoJustica(),
                    request.casoUrgente(),
                    request.preferenciaDigital(),
                    request.processoId()
            );
        }
        var resolucao = competenceResolverService.resolve(new CompetenceResolveRequest(
                null,
                request.assuntoTpu(),
                request.classeTpu(),
                request.materiaPrincipal(),
                request.ufReu() != null ? request.ufReu() : request.ufAutor(),
                request.comarcaReu() != null ? request.comarcaReu() : request.comarcaAutor(),
                request.valorCausa(),
                null,
                null,
                null,
                null,
                null,
                false,
                false,
                false
        ));
        TipoJustica inferida = parseTipoJustica(resolucao.tipoJusticaSugerida());
        return new DynamicRequest(
                request.nupn(),
                request.classeTpu(),
                request.assuntoTpu(),
                request.ramoDireito(),
                request.valorCausa(),
                request.ufAutor(),
                request.comarcaAutor(),
                request.ufReu(),
                request.comarcaReu(),
                request.requerJuizadoEspecial(),
                request.requerVaraEspecializada(),
                request.materiaPrincipal(),
                inferida != null ? inferida : request.tipoJustica(),
                request.casoUrgente(),
                request.preferenciaDigital(),
                request.processoId()
        );
    }

    private static boolean ritoIndicaJuizado(Object rito) {
        if (rito == null) {
            return false;
        }
        String nome = String.valueOf(rito).trim().toUpperCase(Locale.ROOT);
        return nome.equals("JUIZADO_ESPECIAL_CIVEL")
                || nome.equals("JUIZADO_ESPECIAL_FEDERAL")
                || nome.equals("JUIZADO_ESPECIAL_FAZENDA_PUBLICA")
                || nome.equals("JUIZADO_ESPECIAL");
    }

    private static String calcularRequestHash(DynamicRequest request) {
        return Hashes.sha256Hex(String.join("|",
                normalizeHashValue(request.nupn()),
                normalizeHashValue(request.classeTpu()),
                normalizeHashValue(request.assuntoTpu()),
                normalizeHashValue(request.ramoDireito() != null ? request.ramoDireito().name() : null),
                normalizeHashValue(request.valorCausa() != null ? request.valorCausa().setScale(2, RoundingMode.HALF_UP).toPlainString() : null),
                normalizeHashValue(request.ufAutor()),
                normalizeHashValue(request.comarcaAutor()),
                normalizeHashValue(request.ufReu()),
                normalizeHashValue(request.comarcaReu()),
                normalizeHashValue(request.materiaPrincipal()),
                normalizeHashValue(request.tipoJustica() != null ? request.tipoJustica().name() : null),
                normalizeHashValue(String.valueOf(request.requerJuizadoEspecial())),
                normalizeHashValue(String.valueOf(request.requerVaraEspecializada())),
                normalizeHashValue(String.valueOf(request.casoUrgente())),
                normalizeHashValue(String.valueOf(request.preferenciaDigital())),
                normalizeHashValue(request.processoId() != null ? String.valueOf(request.processoId()) : null)
        ));
    }

    private static DynamicRequest sanitizar(DynamicCompetenceDistributionRequest request) {
        if (request == null) {
            return new DynamicRequest(null, null, null, null, null, null, null, null, null, false, false, null, null, false, false, null);
        }
        return new DynamicRequest(
                normalizeText(request.nupn()),
                normalizeUpper(request.classeTpu()),
                normalizeUpper(request.assuntoTpu()),
                parseRamo(request.ramoDireito()),
                request.valorCausa() == null ? null : request.valorCausa().max(BigDecimal.ZERO),
                normalizeUpper(request.ufAutor()),
                normalizeText(request.comarcaAutor()),
                normalizeUpper(request.ufReu()),
                normalizeText(request.comarcaReu()),
                request.requerJuizadoEspecial(),
                request.requerVaraEspecializada(),
                normalizeText(request.materiaPrincipal()),
                parseTipoJustica(request.tipoJustica()),
                request.casoUrgente(),
                request.preferenciaDigital(),
                request.processoId()
        );
    }

    private static TipoJustica parseTipoJustica(String value) {
        String normalized = normalizeUpper(value);
        if (normalized == null) {
            return null;
        }
        return switch (normalized) {
            case "ELEITORAL", "ELEITORAL_SUPERIOR" -> TipoJustica.ELEITORAL;
            case "TRABALHO", "TRABALHO_SUPERIOR" -> TipoJustica.TRABALHO;
            case "FEDERAL" -> TipoJustica.FEDERAL;
            case "MILITAR_ESTADUAL" -> TipoJustica.MILITAR_ESTADUAL;
            case "MILITAR_FEDERAL", "MILITAR_SUPERIOR" -> TipoJustica.MILITAR_FEDERAL;
            case "SUPERIOR", "SUPERIOR_STF" -> TipoJustica.SUPERIOR;
            default -> {
                try {
                    yield TipoJustica.valueOf(normalized);
                } catch (IllegalArgumentException ignored) {
                    yield null;
                }
            }
        };
    }

    private static RamoDireito parseRamo(String value) {
        String normalized = normalizeUpper(value);
        if (normalized == null) {
            return null;
        }
        try {
            return RamoDireito.valueOf(normalized);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static boolean equalOrBlank(String left, String right) {
        String a = normalizeUpper(left);
        String b = normalizeUpper(right);
        if (a == null || b == null) {
            return false;
        }
        return a.equals(b);
    }

    private static boolean ufCompativelOuDesconhecida(String left, String right) {
        String a = normalizeUpper(left);
        String b = normalizeUpper(right);
        if (a == null || b == null) {
            return true;
        }
        return a.equals(b);
    }

    private static String normalizeUpper(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().replaceAll("\\s+", " ");
        return normalized.isBlank() ? null : normalized.toUpperCase(Locale.ROOT);
    }

    private static String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().replaceAll("\\s+", " ");
        return normalized.isBlank() ? null : normalized;
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            String normalized = normalizeText(value);
            if (normalized != null) {
                return normalized;
            }
        }
        return null;
    }

    private static String normalizeHashValue(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private static double round2(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private static String tribunalSigla(UnidadeJudiciariaCompetencia unidade) {
        return unidade.getTribunal() != null ? unidade.getTribunal().getSigla() : null;
    }

    private static String comarcaNome(UnidadeJudiciariaCompetencia unidade) {
        return unidade.getComarcaEntidade() != null ? unidade.getComarcaEntidade().getNome() : unidade.getComarca();
    }

    private static boolean mesmaComarcaResolvida(UnidadeJudiciariaCompetencia item, UnidadeJudiciariaCompetencia origem) {
        Comarca itemEntidade = item.getComarcaEntidade();
        Comarca origemEntidade = origem.getComarcaEntidade();
        if (itemEntidade != null && origemEntidade != null) {
            return Objects.equals(itemEntidade.getId(), origemEntidade.getId());
        }
        String itemNome = normalizeUpper(comarcaNome(item));
        String origemNome = normalizeUpper(comarcaNome(origem));
        if (itemNome == null || origemNome == null) {
            return false;
        }
        return itemNome.equals(origemNome);
    }

    private static String comarcaUf(UnidadeJudiciariaCompetencia unidade) {
        return unidade.getUf();
    }

    record DynamicRequest(
            String nupn,
            String classeTpu,
            String assuntoTpu,
            RamoDireito ramoDireito,
            BigDecimal valorCausa,
            String ufAutor,
            String comarcaAutor,
            String ufReu,
            String comarcaReu,
            boolean requerJuizadoEspecial,
            boolean requerVaraEspecializada,
            String materiaPrincipal,
            TipoJustica tipoJustica,
            boolean casoUrgente,
            boolean preferenciaDigital,
            Long processoId
    ) {
    }

    private record Candidate(
            UnidadeJudiciariaCompetencia unidade,
            double scoreFinal,
            double scoreTerritorial,
            double scoreEspecialidade,
            double scoreDisponibilidade,
            double scoreEquilibrio,
            double scoreAderenciaNormativa,
            List<String> alertas,
            List<String> fatoresRevisao,
            String motivacao
    ) {
    }
}
