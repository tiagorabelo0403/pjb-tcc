package com.tcc.pjb.backend.tribunal.distribuicao;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.util.Hashes;
import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.model.dto.ajuizamento.federal.FederalismoEventoRequest;
import com.tcc.pjb.backend.model.entity.competencia.ModoOperacaoUnidadeJudiciaria;
import com.tcc.pjb.backend.model.entity.competencia.StatusOperacionalUnidadeJudiciaria;
import com.tcc.pjb.backend.model.entity.competencia.TipoVaraDistribuicao;
import com.tcc.pjb.backend.model.entity.competencia.UnidadeJudiciariaCompetencia;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.repository.UnidadeJudiciariaCompetenciaRepository;
import com.tcc.pjb.backend.service.ajuizamento.federal.FederalismoJudicialEngine;
import com.tcc.pjb.backend.service.outbox.OutboxPublisher;
import com.tcc.pjb.backend.tribunal.regras.TribunalRuleEngine;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class ConfiguracaoDistribuicaoVaraService {

    public static final String EVT_DISTRIBUICAO_OCUPACAO_ATUALIZADA = "pjb.tribunal.distribuicao.ocupacao.atualizada";
    public static final String EVT_DISTRIBUICAO_STATUS_ATUALIZADO = "pjb.tribunal.distribuicao.status.atualizado";
    public static final String EVT_DISTRIBUICAO_RESTRICAO_ATUALIZADA = "pjb.tribunal.distribuicao.restricao.atualizada";

    public enum MotivoRestricao {
        MUTIRAO_CONCILIACAO,
        MUTIRAO_CRIMINAL,
        MUTIRAO_EXECUCAO_FISCAL,
        VARA_VAGA,
        VARA_SUBSTITUICAO,
        IMPLANTACAO_SISTEMA,
        MANUTENCAO_TECNICA,
        SUSPENSAO_JUDICIAL,
        FERIAS_COLETIVAS_SERVIDORES,
        REORGANIZACAO_COMPETENCIA,
        SOBRECARGA_CRITICA_AUTO
    }

    public record RestricaoOperacional(
            String varaId,
            boolean aceitaNovasDistribuicoes,
            MotivoRestricao motivoRestricao,
            String descricaoRestricao,
            LocalDate previsaoRetornoNormal,
            boolean prioritaria,
            boolean emMutirao,
            LocalDate mutiraoInicio,
            LocalDate mutiraoFim,
            String descricaoMutirao,
            boolean vagaOuSubstituicao,
            String magistradoId,
            Instant atualizadoEm,
            String atualizadoPor
    ) {
        public RestricaoOperacional {
            varaId = normalizeUpperRequired(varaId, "varaId");
            descricaoRestricao = blankToNull(descricaoRestricao);
            descricaoMutirao = blankToNull(descricaoMutirao);
            magistradoId = blankToNull(magistradoId);
            atualizadoPor = blankToDefault(atualizadoPor, "SISTEMA");
            atualizadoEm = atualizadoEm == null ? Instant.now() : atualizadoEm;
        }

        public boolean mutiraoAtivo(LocalDate dataReferencia) {
            if (!emMutirao) {
                return false;
            }
            LocalDate referencia = dataReferencia == null ? LocalDate.now() : dataReferencia;
            if (mutiraoInicio != null && referencia.isBefore(mutiraoInicio)) {
                return false;
            }
            if (mutiraoFim != null && referencia.isAfter(mutiraoFim)) {
                return false;
            }
            return true;
        }
    }

    public record PerfilVara(
            String varaId,
            String varaDescricao,
            TipoVaraDistribuicao tipoVara,
            String tribunalCodigo,
            String comarcaId,
            String uf,
            TipoJustica tipoJustica,
            RamoDireito ramoDireito,
            boolean aceitaNovasDistribuicoes,
            MotivoRestricao motivoRestricao,
            String descricaoRestricao,
            LocalDate previsaoRetornoNormal,
            Set<String> materiasAceitas,
            Set<String> materiasRecusadas,
            Set<String> ritosAceitos,
            Set<String> classesTPUAceitas,
            Set<String> assuntosTPUAceitos,
            BigDecimal valorMinimoAceito,
            BigDecimal valorMaximoAceito,
            int capacidadeMaxima,
            int processosAtivos,
            BigDecimal indiceCongestionamento,
            BigDecimal limiarCongestionamento,
            boolean prioritaria,
            boolean emMutirao,
            LocalDate mutiraoInicio,
            LocalDate mutiraoFim,
            String descricaoMutirao,
            String magistradoId,
            boolean vagaOuSubstituicao,
            boolean permiteJuizadoEspecial,
            boolean permiteUrgencia,
            boolean permiteDistribuicaoAutomatica,
            boolean somenteDigital,
            ModoOperacaoUnidadeJudiciaria modoOperacao,
            StatusOperacionalUnidadeJudiciaria statusOperacional,
            Instant atualizadoEm,
            String atualizadoPor
    ) {
        public PerfilVara {
            varaId = normalizeUpperRequired(varaId, "varaId");
            varaDescricao = blankToDefault(varaDescricao, varaId);
            tribunalCodigo = normalizeUpperRequired(tribunalCodigo, "tribunalCodigo");
            comarcaId = normalizeText(comarcaId);
            uf = normalizeUpper(uf);
            descricaoRestricao = blankToNull(descricaoRestricao);
            descricaoMutirao = blankToNull(descricaoMutirao);
            magistradoId = blankToNull(magistradoId);
            valorMinimoAceito = sanitizeMoney(valorMinimoAceito);
            valorMaximoAceito = sanitizeMoney(valorMaximoAceito);
            indiceCongestionamento = sanitizeRatio(indiceCongestionamento);
            limiarCongestionamento = sanitizeRatio(limiarCongestionamento == null ? new BigDecimal("0.85") : limiarCongestionamento);
            capacidadeMaxima = Math.max(1, capacidadeMaxima);
            processosAtivos = Math.max(0, processosAtivos);
            materiasAceitas = normalizeSet(materiasAceitas, false);
            materiasRecusadas = normalizeSet(materiasRecusadas, false);
            ritosAceitos = normalizeSet(ritosAceitos, true);
            classesTPUAceitas = normalizeSet(classesTPUAceitas, true);
            assuntosTPUAceitos = normalizeSet(assuntosTPUAceitos, true);
            if (valorMinimoAceito != null && valorMaximoAceito != null && valorMinimoAceito.compareTo(valorMaximoAceito) > 0) {
                BigDecimal swap = valorMinimoAceito;
                valorMinimoAceito = valorMaximoAceito;
                valorMaximoAceito = swap;
            }
            modoOperacao = modoOperacao == null ? ModoOperacaoUnidadeJudiciaria.HIBRIDA : modoOperacao;
            statusOperacional = statusOperacional == null ? StatusOperacionalUnidadeJudiciaria.ATIVA : statusOperacional;
            atualizadoEm = atualizadoEm == null ? Instant.now() : atualizadoEm;
            atualizadoPor = blankToDefault(atualizadoPor, "SISTEMA");
        }

        public double ocupacaoNominal() {
            return round4((double) processosAtivos / (double) Math.max(1, capacidadeMaxima));
        }

        public double ocupacaoEfetiva() {
            return round4(Math.max(ocupacaoNominal(), indiceCongestionamento.doubleValue()));
        }

        public double scoreDisponibilidade() {
            if (!aceitaNovasDistribuicoes || statusOperacional != StatusOperacionalUnidadeJudiciaria.ATIVA) {
                return 0.0d;
            }
            if (ocupacaoEfetiva() >= limiarCongestionamento.doubleValue()) {
                return 0.0d;
            }
            double base = 1.0d - ocupacaoEfetiva();
            if (prioritaria) {
                base += 0.05d;
            }
            if (emMutirao) {
                base -= 0.20d;
            }
            return round4(Math.max(0.0d, Math.min(1.0d, base)));
        }

        public boolean podeReceberProcesso(FiltroDistribuicao filtro) {
            if (filtro == null) {
                return aceitaNovasDistribuicoes && statusOperacional == StatusOperacionalUnidadeJudiciaria.ATIVA;
            }
            if (!aceitaNovasDistribuicoes) {
                return false;
            }
            if (statusOperacional != StatusOperacionalUnidadeJudiciaria.ATIVA) {
                return false;
            }
            if (ocupacaoEfetiva() >= limiarCongestionamento.doubleValue()) {
                return false;
            }
            if (emMutirao && filtro.bloquearMutirao()) {
                return false;
            }
            if (vagaOuSubstituicao && !filtro.aceitaSubstituicao()) {
                return false;
            }
            if (filtro.uf() != null && uf != null && !Objects.equals(uf, normalizeUpper(filtro.uf()))) {
                return false;
            }
            if (filtro.comarcaId() != null && comarcaId != null && !Objects.equals(comarcaId, normalizeText(filtro.comarcaId()))) {
                return false;
            }
            if (filtro.requerJuizadoEspecializado() && !permiteJuizadoEspecial) {
                return false;
            }
            if (filtro.casoUrgente() && !permiteUrgencia) {
                return false;
            }
            if (filtro.preferenciaDigital() && modoOperacao == ModoOperacaoUnidadeJudiciaria.PRESENCIAL) {
                return false;
            }
            if (somenteDigital && !filtro.preferenciaDigital()) {
                return false;
            }
            if (filtro.materia() != null) {
                String materia = normalizeToken(filtro.materia());
                if (!materiasAceitas.isEmpty() && !materiasAceitas.contains(materia)) {
                    return false;
                }
                if (materiasRecusadas.contains(materia)) {
                    return false;
                }
                if (ramoDireito != null && !ramoCompativelComMateria(ramoDireito, materia)) {
                    return false;
                }
            }
            if (filtro.rito() != null && !ritosAceitos.isEmpty() && !ritosAceitos.contains(normalizeToken(filtro.rito()))) {
                return false;
            }
            if (filtro.classeTPU() != null && !classesTPUAceitas.isEmpty() && !classesTPUAceitas.contains(normalizeToken(filtro.classeTPU()))) {
                return false;
            }
            if (filtro.assuntoTPU() != null && !assuntosTPUAceitos.isEmpty() && !assuntosTPUAceitos.contains(normalizeToken(filtro.assuntoTPU()))) {
                return false;
            }
            if (filtro.valorCausa() != null) {
                BigDecimal valor = sanitizeMoney(filtro.valorCausa());
                if (valorMinimoAceito != null && valor.compareTo(valorMinimoAceito) < 0) {
                    return false;
                }
                if (valorMaximoAceito != null && valor.compareTo(valorMaximoAceito) > 0) {
                    return false;
                }
            }
            return true;
        }

        public String motivoRecusa(FiltroDistribuicao filtro) {
            if (!aceitaNovasDistribuicoes) {
                String base = descricaoRestricao != null ? descricaoRestricao : motivoRestricao != null ? motivoRestricao.name() : "indisponivel";
                return previsaoRetornoNormal == null ? "Vara indisponivel: " + base : "Vara indisponivel: " + base + " | previsao=" + previsaoRetornoNormal;
            }
            if (statusOperacional != StatusOperacionalUnidadeJudiciaria.ATIVA) {
                return "Status operacional impeditivo: " + statusOperacional.name();
            }
            if (ocupacaoEfetiva() >= limiarCongestionamento.doubleValue()) {
                return "Congestionamento acima do limiar: " + percentual(ocupacaoEfetiva()) + " / limiar=" + percentual(limiarCongestionamento.doubleValue());
            }
            if (filtro != null && emMutirao && filtro.bloquearMutirao()) {
                return descricaoMutirao == null ? "Vara em mutirao" : "Vara em mutirao: " + descricaoMutirao;
            }
            if (filtro != null && vagaOuSubstituicao && !filtro.aceitaSubstituicao()) {
                return "Vara em vacancia ou substituicao";
            }
            if (filtro != null && filtro.materia() != null) {
                String materia = normalizeToken(filtro.materia());
                if (!materiasAceitas.isEmpty() && !materiasAceitas.contains(materia)) {
                    return "Materia fora do escopo da vara";
                }
                if (materiasRecusadas.contains(materia)) {
                    return "Materia recusada pela configuracao da vara";
                }
            }
            if (filtro != null && filtro.rito() != null && !ritosAceitos.isEmpty() && !ritosAceitos.contains(normalizeToken(filtro.rito()))) {
                return "Rito processual nao aceito";
            }
            if (filtro != null && filtro.classeTPU() != null && !classesTPUAceitas.isEmpty() && !classesTPUAceitas.contains(normalizeToken(filtro.classeTPU()))) {
                return "Classe TPU nao aceita";
            }
            if (filtro != null && filtro.assuntoTPU() != null && !assuntosTPUAceitos.isEmpty() && !assuntosTPUAceitos.contains(normalizeToken(filtro.assuntoTPU()))) {
                return "Assunto TPU nao aceito";
            }
            if (filtro != null && filtro.valorCausa() != null && valorMaximoAceito != null && sanitizeMoney(filtro.valorCausa()).compareTo(valorMaximoAceito) > 0) {
                return "Valor da causa acima do teto aceito";
            }
            return "Processo fora dos criterios operacionais da vara";
        }

    }

    public record FiltroDistribuicao(
            String materia,
            String rito,
            String classeTPU,
            String assuntoTPU,
            BigDecimal valorCausa,
            boolean requerJuizadoEspecializado,
            boolean aceitaSubstituicao,
            String uf,
            String comarcaId,
            boolean casoUrgente,
            boolean preferenciaDigital,
            boolean bloquearMutirao
    ) {
        public FiltroDistribuicao {
            materia = normalizeText(materia);
            rito = normalizeUpper(rito);
            classeTPU = normalizeUpper(classeTPU);
            assuntoTPU = normalizeUpper(assuntoTPU);
            valorCausa = sanitizeMoney(valorCausa);
            uf = normalizeUpper(uf);
            comarcaId = normalizeText(comarcaId);
        }

        public static FiltroDistribuicao simples(String materia, String rito, BigDecimal valor) {
            return new FiltroDistribuicao(materia, rito, null, null, valor, false, true, null, null, false, false, true);
        }
    }

    public record ResultadoConsulta(
            String varaId,
            boolean aceita,
            String motivoRecusa,
            PerfilVara perfil,
            List<String> varasAlternativas,
            Instant consultadoEm
    ) {
    }

    public record SugestaoRedistribuicao(
            String varaOrigem,
            String varaDestino,
            int processosPropostos,
            double ocupacaoOrigemAtual,
            double ocupacaoDestinoEstimada,
            String justificativa,
            boolean requerAprovacaoHumana
    ) {
    }

    public record AtualizacaoOcupacao(
            String varaId,
            boolean sucesso,
            String mensagem,
            int processosAtivos,
            double ocupacaoEfetiva,
            boolean distribuicaoBloqueada,
            Instant processadoEm
    ) {
    }

    public record DashboardDistribuicao(
            String tribunalCodigo,
            long totalVaras,
            long varasAceitando,
            long varasFechadas,
            long varasCriticas,
            long varasEmMutirao,
            long varasVagas,
            long varasPrioritarias,
            long capacidadeTotal,
            long processosAtivosTotais,
            double ocupacaoMedia,
            double ocupacaoGlobal,
            Map<TipoVaraDistribuicao, Long> distribuicaoPorTipo,
            Map<MotivoRestricao, Long> motivosRestricao,
            List<PerfilVara> topVarasCriticas,
            Instant geradoEm
    ) {
        public DashboardDistribuicao {
            capacidadeTotal = Math.max(0L, capacidadeTotal);
            processosAtivosTotais = Math.max(0L, processosAtivosTotais);
            ocupacaoMedia = round4(Math.max(0.0d, ocupacaoMedia));
            ocupacaoGlobal = round4(Math.max(0.0d, ocupacaoGlobal));
            distribuicaoPorTipo = Map.copyOf(distribuicaoPorTipo == null ? Map.of() : distribuicaoPorTipo);
            motivosRestricao = Map.copyOf(motivosRestricao == null ? Map.of() : motivosRestricao);
            topVarasCriticas = List.copyOf(topVarasCriticas == null ? List.of() : topVarasCriticas);
            geradoEm = geradoEm == null ? Instant.now() : geradoEm;
        }
    }

    private final UnidadeJudiciariaCompetenciaRepository unidadeRepository;
    private final TribunalRuleEngine tribunalRuleEngine;
    private final FederalismoJudicialEngine federalismoJudicialEngine;
    private final OutboxPublisher outboxPublisher;
    private final ObjectMapper objectMapper;

    private final Map<String, RestricaoOperacional> restricoesOperacionais = new ConcurrentHashMap<>();
    private final Map<String, PerfilVara> perfisCache = new ConcurrentHashMap<>();
    private final List<AtualizacaoOcupacao> historicoOcupacao = new CopyOnWriteArrayList<>();

    public ConfiguracaoDistribuicaoVaraService(UnidadeJudiciariaCompetenciaRepository unidadeRepository,
                                               TribunalRuleEngine tribunalRuleEngine,
                                               FederalismoJudicialEngine federalismoJudicialEngine,
                                               OutboxPublisher outboxPublisher,
                                               ObjectMapper objectMapper) {
        this.unidadeRepository = Objects.requireNonNull(unidadeRepository);
        this.tribunalRuleEngine = Objects.requireNonNull(tribunalRuleEngine);
        this.federalismoJudicialEngine = Objects.requireNonNull(federalismoJudicialEngine);
        this.outboxPublisher = Objects.requireNonNull(outboxPublisher);
        this.objectMapper = Objects.requireNonNull(objectMapper);
    }

    @PostConstruct
    public void inicializar() {
        recarregarDoRepositorio();
    }

    @Transactional(readOnly = true)
    public int recarregarDoRepositorio() {
        LinkedHashMap<String, PerfilVara> novos = new LinkedHashMap<>();
        LinkedHashSet<String> codigosAtivos = new LinkedHashSet<>();
        for (UnidadeJudiciariaCompetencia unidade : unidadeRepository.findAll()) {
            String codigo = normalizeUpper(unidade.getCodigo());
            codigosAtivos.add(codigo);
            PerfilVara perfil = toPerfil(unidade, restricoesOperacionais.get(codigo));
            novos.put(perfil.varaId(), perfil);
        }
        restricoesOperacionais.keySet().removeIf(codigo -> !codigosAtivos.contains(codigo));
        perfisCache.clear();
        perfisCache.putAll(novos);
        return novos.size();
    }

    @Transactional(readOnly = true)
    public Optional<PerfilVara> buscarPerfil(String varaId) {
        String codigo = normalizeUpper(vagaToNull(varaId));
        if (codigo == null) {
            return Optional.empty();
        }
        PerfilVara cache = perfisCache.get(codigo);
        if (cache != null) {
            return Optional.of(cache);
        }
        return unidadeRepository.findByCodigo(codigo).map(unidade -> {
            PerfilVara perfil = toPerfil(unidade, restricoesOperacionais.get(codigo));
            perfisCache.put(perfil.varaId(), perfil);
            return perfil;
        });
    }

    @Transactional(readOnly = true)
    public ResultadoConsulta consultar(String varaId, FiltroDistribuicao filtro) {
        Optional<PerfilVara> perfil = buscarPerfil(varaId);
        if (perfil.isEmpty()) {
            return new ResultadoConsulta(normalizeUpper(varaId), false, "Vara nao cadastrada", null, List.of(), Instant.now());
        }
        PerfilVara p = perfil.get();
        boolean aceita = p.podeReceberProcesso(filtro);
        String motivo = aceita ? null : p.motivoRecusa(filtro);
        List<String> alternativas = aceita ? List.of() : encontrarAlternativas(filtro, p.tribunalCodigo(), p.comarcaId(), p.uf(), p.varaId(), 5);
        return new ResultadoConsulta(p.varaId(), aceita, motivo, p, alternativas, Instant.now());
    }

    @Transactional(readOnly = true)
    public List<PerfilVara> varasDisponiveisNaComarca(String comarcaId, FiltroDistribuicao filtro) {
        String comarca = normalizeText(comarcaId);
        if (comarca == null) {
            return List.of();
        }
        return perfisCache.values().stream()
                .filter(item -> Objects.equals(item.comarcaId(), comarca))
                .filter(item -> item.podeReceberProcesso(filtro))
                .sorted(Comparator.comparingDouble(PerfilVara::scoreDisponibilidade).reversed()
                        .thenComparing(Comparator.comparingDouble(PerfilVara::ocupacaoEfetiva))
                        .thenComparing(PerfilVara::varaDescricao))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SugestaoRedistribuicao> sugerirRedistribuicao(String comarcaId, String tribunalCodigo) {
        String comarca = normalizeText(comarcaId);
        String tribunal = normalizeUpper(tribunalCodigo);
        if (tribunal == null || comarca == null) {
            return List.of();
        }
        List<PerfilVara> perfis = perfisCache.values().stream()
                .filter(item -> Objects.equals(item.tribunalCodigo(), tribunal))
                .filter(item -> Objects.equals(item.comarcaId(), comarca))
                .toList();
        if (perfis.isEmpty()) {
            return List.of();
        }
        List<SugestaoRedistribuicao> sugestoes = new ArrayList<>();
        List<PerfilVara> sobrecarregadas = perfis.stream()
                .filter(item -> item.ocupacaoEfetiva() >= item.limiarCongestionamento().doubleValue())
                .sorted(Comparator.comparingDouble(PerfilVara::ocupacaoEfetiva).reversed())
                .toList();
        for (PerfilVara origem : sobrecarregadas) {
            int metaOrigem = Math.max(1, (int) Math.floor(origem.capacidadeMaxima() * 0.75d));
            int excedente = Math.max(0, origem.processosAtivos() - metaOrigem);
            if (excedente == 0) {
                continue;
            }
            List<PerfilVara> destinos = perfis.stream()
                    .filter(item -> !Objects.equals(item.varaId(), origem.varaId()))
                    .filter(item -> item.podeReceberProcesso(new FiltroDistribuicao(null, null, null, null, null, false, true, origem.uf(), origem.comarcaId(), false, false, false)))
                    .filter(item -> item.ocupacaoEfetiva() < 0.60d)
                    .filter(item -> tipoCompativel(origem.tipoVara(), item.tipoVara()))
                    .sorted(Comparator.comparingDouble(PerfilVara::ocupacaoEfetiva).thenComparing(PerfilVara::varaDescricao))
                    .toList();
            for (PerfilVara destino : destinos) {
                int capacidadeDestino = Math.max(0, (int) Math.floor(destino.capacidadeMaxima() * 0.75d) - destino.processosAtivos());
                int transferir = Math.min(excedente, capacidadeDestino);
                if (transferir <= 0) {
                    continue;
                }
                double ocupacaoDestinoEstimada = round4((double) (destino.processosAtivos() + transferir) / (double) Math.max(1, destino.capacidadeMaxima()));
                sugestoes.add(new SugestaoRedistribuicao(
                        origem.varaId(),
                        destino.varaId(),
                        transferir,
                        origem.ocupacaoEfetiva(),
                        ocupacaoDestinoEstimada,
                        "Redistribuicao sugerida para equalizar carga entre unidades compativeis da mesma comarca.",
                        true
                ));
                break;
            }
        }
        return List.copyOf(sugestoes);
    }

    @Transactional
    public AtualizacaoOcupacao atualizarOcupacao(String varaId, int processosAtivos, String operador) {
        String codigo = normalizeUpperRequired(varaId, "varaId");
        UnidadeJudiciariaCompetencia unidade = unidadeRepository.findByCodigo(codigo)
                .orElse(null);
        if (unidade == null) {
            AtualizacaoOcupacao resultado = new AtualizacaoOcupacao(codigo, false, "Vara nao cadastrada", 0, 0.0d, false, Instant.now());
            registrarHistorico(resultado);
            return resultado;
        }
        int novosAtivos = Math.max(0, processosAtivos);
        BigDecimal congestionamento = BigDecimal.valueOf((double) novosAtivos / (double) Math.max(1, unidade.getCapacidadeMaxima()))
                .setScale(4, RoundingMode.HALF_UP)
                .min(BigDecimal.ONE)
                .max(BigDecimal.ZERO);
        unidade.atualizarCarga(novosAtivos, congestionamento);

        TribunalRuleEngine.ContextoResolucao contexto = TribunalRuleEngine.ContextoResolucao.agora(unidade.getTribunalCodigo(), unidade.getComarca(), unidade.getCodigo(), unidade.getRamoDireito(), null);
        BigDecimal limiar = tribunalRuleEngine.resolverLimiarCongestionamento(contexto, new BigDecimal("0.85"));
        boolean bloquear = congestionamento.compareTo(limiar) >= 0;

        RestricaoOperacional restricaoAtual = restricoesOperacionais.get(codigo);
        boolean autoAtual = restricaoAtual != null && restricaoAtual.motivoRestricao() == MotivoRestricao.SOBRECARGA_CRITICA_AUTO;
        if (bloquear) {
            unidade.setAceitaDistribuicao(false);
            unidade.setStatusOperacional(StatusOperacionalUnidadeJudiciaria.SATURADA);
            restricoesOperacionais.put(codigo, new RestricaoOperacional(
                    codigo,
                    false,
                    MotivoRestricao.SOBRECARGA_CRITICA_AUTO,
                    "Distribuicao suspensa automaticamente por congestionamento acima do limiar configurado.",
                    null,
                    restricaoAtual != null && restricaoAtual.prioritaria(),
                    restricaoAtual != null && restricaoAtual.emMutirao(),
                    restricaoAtual != null ? restricaoAtual.mutiraoInicio() : null,
                    restricaoAtual != null ? restricaoAtual.mutiraoFim() : null,
                    restricaoAtual != null ? restricaoAtual.descricaoMutirao() : null,
                    restricaoAtual != null && restricaoAtual.vagaOuSubstituicao(),
                    restricaoAtual != null ? restricaoAtual.magistradoId() : null,
                    Instant.now(),
                    blankToDefault(operador, "SISTEMA-WORKER-OCUPACAO")
            ));
        } else if (autoAtual) {
            unidade.setAceitaDistribuicao(true);
            unidade.setStatusOperacional(StatusOperacionalUnidadeJudiciaria.ATIVA);
            restricoesOperacionais.remove(codigo);
        }
        unidadeRepository.save(unidade);
        PerfilVara perfil = toPerfil(unidade, restricoesOperacionais.get(codigo));
        perfisCache.put(perfil.varaId(), perfil);

        AtualizacaoOcupacao resultado = new AtualizacaoOcupacao(
                codigo,
                true,
                bloquear ? "Distribuicao bloqueada por congestionamento" : autoAtual ? "Vara normalizada" : "Ocupacao atualizada",
                novosAtivos,
                perfil.ocupacaoEfetiva(),
                bloquear,
                Instant.now()
        );
        registrarHistorico(resultado);
        Map<String, Object> payloadOcupacao = new LinkedHashMap<>();
        payloadOcupacao.put("varaId", codigo);
        payloadOcupacao.put("processosAtivos", novosAtivos);
        payloadOcupacao.put("ocupacaoEfetiva", perfil.ocupacaoEfetiva());
        payloadOcupacao.put("limiarCongestionamento", perfil.limiarCongestionamento());
        payloadOcupacao.put("aceitaNovasDistribuicoes", perfil.aceitaNovasDistribuicoes());
        payloadOcupacao.put("atualizadoEm", resultado.processadoEm().toString());
        publicarEventoOcorrencia(EVT_DISTRIBUICAO_OCUPACAO_ATUALIZADA, perfil.tribunalCodigo(), codigo, payloadOcupacao, blankToDefault(operador, "SISTEMA-WORKER-OCUPACAO"));
        return resultado;
    }

    @Transactional
    public boolean alterarStatusDistribuicao(String varaId,
                                             boolean aceita,
                                             MotivoRestricao motivo,
                                             String descricao,
                                             LocalDate previsaoRetorno,
                                             String operador) {
        String codigo = normalizeUpperRequired(varaId, "varaId");
        UnidadeJudiciariaCompetencia unidade = unidadeRepository.findByCodigo(codigo).orElse(null);
        if (unidade == null) {
            return false;
        }
        RestricaoOperacional atual = restricoesOperacionais.get(codigo);
        RestricaoOperacional nova = new RestricaoOperacional(
                codigo,
                aceita,
                aceita ? null : motivo,
                aceita ? null : descricao,
                aceita ? null : previsaoRetorno,
                atual != null && atual.prioritaria(),
                atual != null && atual.emMutirao(),
                atual != null ? atual.mutiraoInicio() : null,
                atual != null ? atual.mutiraoFim() : null,
                atual != null ? atual.descricaoMutirao() : null,
                atual != null && atual.vagaOuSubstituicao(),
                atual != null ? atual.magistradoId() : null,
                Instant.now(),
                blankToDefault(operador, "ADMIN")
        );
        if (aceita) {
            restricoesOperacionais.remove(codigo);
            unidade.setAceitaDistribuicao(true);
            unidade.setStatusOperacional(StatusOperacionalUnidadeJudiciaria.ATIVA);
        } else {
            restricoesOperacionais.put(codigo, nova);
            unidade.setAceitaDistribuicao(false);
            unidade.setStatusOperacional(StatusOperacionalUnidadeJudiciaria.BLOQUEADA);
        }
        unidadeRepository.save(unidade);
        PerfilVara perfil = toPerfil(unidade, restricoesOperacionais.get(codigo));
        perfisCache.put(perfil.varaId(), perfil);
        Map<String, Object> payloadStatus = new LinkedHashMap<>();
        payloadStatus.put("varaId", codigo);
        payloadStatus.put("aceitaNovasDistribuicoes", perfil.aceitaNovasDistribuicoes());
        payloadStatus.put("motivoRestricao", perfil.motivoRestricao() == null ? null : perfil.motivoRestricao().name());
        payloadStatus.put("descricaoRestricao", perfil.descricaoRestricao());
        payloadStatus.put("previsaoRetornoNormal", perfil.previsaoRetornoNormal() == null ? null : perfil.previsaoRetornoNormal().toString());
        payloadStatus.put("atualizadoEm", perfil.atualizadoEm().toString());
        publicarEventoOcorrencia(EVT_DISTRIBUICAO_STATUS_ATUALIZADO, perfil.tribunalCodigo(), codigo, payloadStatus, blankToDefault(operador, "ADMIN"));
        return true;
    }

    @Transactional
    public PerfilVara atualizarRestricaoOperacional(RestricaoOperacional restricao) {
        Objects.requireNonNull(restricao, "restricao");
        UnidadeJudiciariaCompetencia unidade = unidadeRepository.findByCodigo(restricao.varaId())
                .orElseThrow(() -> new IllegalArgumentException("Vara nao cadastrada"));
        restricoesOperacionais.put(restricao.varaId(), restricao);
        unidade.setAceitaDistribuicao(restricao.aceitaNovasDistribuicoes());
        unidade.setStatusOperacional(restricao.aceitaNovasDistribuicoes()
                ? StatusOperacionalUnidadeJudiciaria.ATIVA
                : StatusOperacionalUnidadeJudiciaria.BLOQUEADA);
        unidadeRepository.save(unidade);
        PerfilVara perfil = toPerfil(unidade, restricao);
        perfisCache.put(perfil.varaId(), perfil);
        Map<String, Object> payloadRestricao = new LinkedHashMap<>();
        payloadRestricao.put("varaId", perfil.varaId());
        payloadRestricao.put("motivoRestricao", perfil.motivoRestricao() == null ? null : perfil.motivoRestricao().name());
        payloadRestricao.put("descricaoRestricao", perfil.descricaoRestricao());
        payloadRestricao.put("emMutirao", perfil.emMutirao());
        payloadRestricao.put("vagaOuSubstituicao", perfil.vagaOuSubstituicao());
        payloadRestricao.put("prioritaria", perfil.prioritaria());
        payloadRestricao.put("atualizadoEm", perfil.atualizadoEm().toString());
        publicarEventoOcorrencia(EVT_DISTRIBUICAO_RESTRICAO_ATUALIZADA, perfil.tribunalCodigo(), perfil.varaId(), payloadRestricao, perfil.atualizadoPor());
        return perfil;
    }

    @Transactional(readOnly = true)
    public DashboardDistribuicao dashboard(String tribunalCodigo) {
        String tribunal = normalizeUpperRequired(tribunalCodigo, "tribunalCodigo");
        List<PerfilVara> varas = perfisCache.values().stream()
                .filter(item -> Objects.equals(item.tribunalCodigo(), tribunal))
                .sorted(Comparator.comparingDouble(PerfilVara::ocupacaoEfetiva).reversed())
                .toList();
        long fechadas = varas.stream().filter(item -> !item.aceitaNovasDistribuicoes()).count();
        long criticas = varas.stream().filter(item -> item.ocupacaoEfetiva() >= item.limiarCongestionamento().doubleValue()).count();
        long mutirao = varas.stream().filter(PerfilVara::emMutirao).count();
        long vagas = varas.stream().filter(PerfilVara::vagaOuSubstituicao).count();
        long prioritarias = varas.stream().filter(PerfilVara::prioritaria).count();
        long capacidadeTotal = varas.stream().mapToLong(PerfilVara::capacidadeMaxima).sum();
        long processosAtivosTotais = varas.stream().mapToLong(PerfilVara::processosAtivos).sum();
        double ocupacaoMedia = round4(varas.stream().mapToDouble(PerfilVara::ocupacaoEfetiva).average().orElse(0.0d));
        double ocupacaoGlobal = capacidadeTotal > 0L ? round4((double) processosAtivosTotais / (double) capacidadeTotal) : 0.0d;
        Map<TipoVaraDistribuicao, Long> porTipo = varas.stream()
                .collect(java.util.stream.Collectors.groupingBy(PerfilVara::tipoVara, LinkedHashMap::new, java.util.stream.Collectors.counting()));
        Map<MotivoRestricao, Long> porMotivo = varas.stream()
                .filter(item -> item.motivoRestricao() != null)
                .collect(java.util.stream.Collectors.groupingBy(PerfilVara::motivoRestricao, () -> new EnumMap<>(MotivoRestricao.class), java.util.stream.Collectors.counting()));
        return new DashboardDistribuicao(
                tribunal,
                varas.size(),
                varas.size() - fechadas,
                fechadas,
                criticas,
                mutirao,
                vagas,
                prioritarias,
                capacidadeTotal,
                processosAtivosTotais,
                ocupacaoMedia,
                ocupacaoGlobal,
                Map.copyOf(porTipo),
                Map.copyOf(porMotivo),
                varas.stream().limit(10).toList(),
                Instant.now()
        );
    }

    @Transactional(readOnly = true)
    public List<AtualizacaoOcupacao> historicoOcupacaoRecente(int limite) {
        int max = Math.max(1, limite);
        List<AtualizacaoOcupacao> copia = new ArrayList<>(historicoOcupacao);
        copia.sort(Comparator.comparing(AtualizacaoOcupacao::processadoEm).reversed());
        return copia.stream().limit(max).toList();
    }

    private PerfilVara toPerfil(UnidadeJudiciariaCompetencia unidade, RestricaoOperacional restricao) {
        TribunalRuleEngine.ContextoResolucao contexto = TribunalRuleEngine.ContextoResolucao.agora(
                unidade.getTribunalCodigo(),
                unidade.getComarca(),
                unidade.getCodigo(),
                unidade.getRamoDireito(),
                null
        );
        int capacidade = unidade.getCapacidadeMaxima() > 0
                ? unidade.getCapacidadeMaxima()
                : tribunalRuleEngine.resolverCapacidadeMaximaVara(contexto, 1);
        BigDecimal limiar = tribunalRuleEngine.resolverLimiarCongestionamento(contexto, new BigDecimal("0.85"));
        return new PerfilVara(
                normalizeUpper(unidade.getCodigo()),
                unidade.getCodigo(),
                unidade.getTipoVara(),
                unidade.getTribunalCodigo(),
                unidade.getComarca(),
                unidade.getUf(),
                unidade.getTipoJustica(),
                unidade.getRamoDireito(),
                restricao == null ? unidade.isAceitaDistribuicao() : restricao.aceitaNovasDistribuicoes(),
                restricao == null ? (unidade.isAceitaDistribuicao() ? null : MotivoRestricao.SUSPENSAO_JUDICIAL) : restricao.motivoRestricao(),
                restricao == null ? null : restricao.descricaoRestricao(),
                restricao == null ? null : restricao.previsaoRetornoNormal(),
                unidade.getEspecialidades(),
                Set.of(),
                Set.of(),
                unidade.getClassesTpu(),
                unidade.getAssuntosTpu(),
                unidade.getValorCausaMinimo(),
                unidade.getValorCausaMaximo(),
                capacidade,
                unidade.getProcessosAtivos(),
                unidade.getIndiceCongestionamento(),
                limiar,
                restricao != null && restricao.prioritaria(),
                restricao != null && restricao.mutiraoAtivo(LocalDate.now()),
                restricao == null ? null : restricao.mutiraoInicio(),
                restricao == null ? null : restricao.mutiraoFim(),
                restricao == null ? null : restricao.descricaoMutirao(),
                restricao == null ? null : restricao.magistradoId(),
                restricao != null && restricao.vagaOuSubstituicao(),
                unidade.isPermiteJuizadoEspecial(),
                unidade.isPermiteUrgencia(),
                unidade.isPermiteDistribuicaoAutomatica(),
                unidade.isSomenteDigital(),
                unidade.getModoOperacao(),
                unidade.getStatusOperacional(),
                restricao == null ? unidade.getAtualizadoEm() : restricao.atualizadoEm(),
                restricao == null ? "REPOSITORIO" : restricao.atualizadoPor()
        );
    }

    private List<String> encontrarAlternativas(FiltroDistribuicao filtro,
                                               String tribunalCodigo,
                                               String comarcaId,
                                               String uf,
                                               String varaExcluida,
                                               int limite) {
        return perfisCache.values().stream()
                .filter(item -> !Objects.equals(item.varaId(), varaExcluida))
                .filter(item -> Objects.equals(item.tribunalCodigo(), tribunalCodigo))
                .filter(item -> Objects.equals(item.comarcaId(), comarcaId) || Objects.equals(item.uf(), uf))
                .filter(item -> item.podeReceberProcesso(filtro))
                .sorted(Comparator.comparingDouble(PerfilVara::scoreDisponibilidade).reversed())
                .limit(Math.max(1, limite))
                .map(item -> item.varaId() + " (" + item.varaDescricao() + " | ocupacao=" + percentual(item.ocupacaoEfetiva()) + ")")
                .toList();
    }

    private void publicarEventoOcorrencia(String eventType,
                                          String tribunalCodigo,
                                          String varaId,
                                          Map<String, Object> payload,
                                          String operador) {
        Map<String, Object> body = new LinkedHashMap<>(payload);
        body.put("tribunalCodigo", tribunalCodigo);
        body.put("varaId", varaId);
        body.put("geradoEm", Instant.now().toString());
        String dedup = "varaDistribuicao:" + eventType + ":" + varaId + ":" + Hashes.sha256Hex(body.toString());
        outboxPublisher.enqueue(
                "tribunal-distribuicao:" + tribunalCodigo,
                eventType,
                body,
                Map.of("source", "configuracao_distribuicao_vara"),
                dedup,
                "ConfiguracaoDistribuicaoVara",
                varaId
        );
        federarMelhorEsforco(eventType, tribunalCodigo, operador, varaId, body, dedup);
    }

    private void federarMelhorEsforco(String eventType,
                                      String tribunalCodigo,
                                      String operador,
                                      String correlationId,
                                      Map<String, Object> payload,
                                      String dedup) {
        try {
            federalismoJudicialEngine.registrarEventoFederado(new FederalismoEventoRequest(
                    tribunalCodigo,
                    FederalismoJudicialEngine.TOPIC_FEDERACAO_EVENTOS,
                    eventType,
                    null,
                    operador,
                    correlationId,
                    dedup,
                    FederalismoJudicialEngine.SCHEMA_VERSION_ATUAL,
                    6,
                    Boolean.FALSE,
                    objectMapper.writeValueAsString(payload),
                    Map.of("source", "configuracao_distribuicao_vara")
            ));
        } catch (Exception ignored) {
        }
    }

    private void registrarHistorico(AtualizacaoOcupacao atualizacao) {
        historicoOcupacao.add(atualizacao);
        if (historicoOcupacao.size() > 2000) {
            historicoOcupacao.remove(0);
        }
    }

    private static boolean tipoCompativel(TipoVaraDistribuicao origem, TipoVaraDistribuicao destino) {
        if (origem == destino) {
            return true;
        }
        if (destino == TipoVaraDistribuicao.VARA_UNICA) {
            return true;
        }
        return switch (origem) {
            case CIVEL_GERAL, CIVEL_FAMILIA, CIVEL_EMPRESARIAL, FAZENDA_PUBLICA, EXECUCAO_FISCAL ->
                    destino == TipoVaraDistribuicao.CIVEL_GERAL || destino == TipoVaraDistribuicao.FAZENDA_PUBLICA || destino == TipoVaraDistribuicao.EXECUCAO_FISCAL;
            case CRIMINAL_GERAL, CRIMINAL_JUIZADO, TRIBUNAL_JURI, VIOLENCIA_DOMESTICA, INFANCIA_JUVENTUDE ->
                    destino == TipoVaraDistribuicao.CRIMINAL_GERAL || destino == TipoVaraDistribuicao.CRIMINAL_JUIZADO || destino == TipoVaraDistribuicao.VIOLENCIA_DOMESTICA;
            case TRABALHO -> true;
            case MEIO_AMBIENTE -> true;
            case SAUDE_PUBLICA -> true;
            case VARA_UNICA -> true;
            default -> false;
        };
    }

    private static boolean ramoCompativelComMateria(RamoDireito ramo, String materia) {
        return switch (ramo) {
            case CIVIL -> !(materia.contains("PENAL") || materia.contains("CRIM") || materia.contains("TRABALH"));
            case FAMILIA -> materia.contains("FAMIL") || materia.contains("ALIMENT") || materia.contains("SUCESS");
            case CONSUMIDOR -> materia.contains("CONSUM") || materia.contains("CDC");
            case EMPRESARIAL -> materia.contains("EMPRES") || materia.contains("FALEN") || materia.contains("RECUPER");
            case PENAL -> materia.contains("PENAL") || materia.contains("CRIM") || materia.contains("JURI") || materia.contains("INFRAC");
            case MILITAR -> materia.contains("MILITAR");
            case ELEITORAL -> materia.contains("ELEIT");
            case ADMINISTRATIVO -> materia.contains("ADMIN") || materia.contains("SERVIDOR") || materia.contains("IMPROB");
            case TRIBUTARIO -> materia.contains("TRIBUT") || materia.contains("FISCAL");
            case CONSTITUCIONAL -> materia.contains("CONSTIT") || materia.contains("MANDADO") || materia.contains("ADI") || materia.contains("ADC");
            case AMBIENTAL -> materia.contains("AMBI") || materia.contains("FLOREST") || materia.contains("POLU");
            case TRABALHISTA -> materia.contains("TRABALH") || materia.contains("CLT");
            case PREVIDENCIARIO -> materia.contains("PREVID") || materia.contains("INSS") || materia.contains("BENEF");
            case INFANCIA_JUVENTUDE -> materia.contains("INFANC") || materia.contains("ADOCA") || materia.contains("MENOR") || materia.contains("ECA");
            case AGRARIO -> materia.contains("AGRAR") || materia.contains("RURAL") || materia.contains("TERRA");
            default -> false;
        };
    }

    private static Set<String> normalizeSet(Collection<String> values, boolean upper) {
        if (values == null || values.isEmpty()) {
            return Set.of();
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String value : values) {
            String token = upper ? normalizeUpper(value) : normalizeToken(value);
            if (token != null) {
                normalized.add(token);
            }
        }
        return Set.copyOf(normalized);
    }

    private static BigDecimal sanitizeMoney(BigDecimal value) {
        if (value == null) {
            return null;
        }
        return value.max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal sanitizeRatio(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value.max(BigDecimal.ZERO).min(BigDecimal.ONE)).setScale(4, RoundingMode.HALF_UP);
    }

    private static String percentual(double value) {
        return BigDecimal.valueOf(value * 100.0d).setScale(0, RoundingMode.HALF_UP) + "%";
    }

    private static String normalizeUpperRequired(String value, String field) {
        String normalized = normalizeUpper(value);
        if (normalized == null) {
            throw new IllegalArgumentException("Campo obrigatorio ausente: " + field);
        }
        return normalized;
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

    private static String normalizeToken(String value) {
        String normalized = normalizeText(value);
        if (normalized == null) {
            return null;
        }
        String token = Normalizer.normalize(normalized, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_+", "")
                .replaceAll("_+$", "");
        return switch (token) {
            case "CIVEL" -> "CIVIL";
            case "TRIBUTARIA" -> "TRIBUTARIO";
            case "PREVIDENCIARIA" -> "PREVIDENCIARIO";
            case "ACAO_DE_COBRANCA", "PETICAO_INICIAL" -> "PROCEDIMENTO_COMUM_CIVEL";
            case "COBRANCA", "COBRANCA_CONTRATUAL", "CONTRATO_INADIMPLIDO", "CONTRATUAL" -> "CONTRATOS";
            case "OBRIGACAO_CONTRATUAL_VENCIDA", "OBRIGACOES_CONTRATUAIS" -> "OBRIGACOES";
            default -> token;
        };
    }

    private static String blankToNull(String value) {
        return normalizeText(value);
    }

    private static String blankToDefault(String value, String fallback) {
        String normalized = blankToNull(value);
        return normalized == null ? fallback : normalized;
    }

    private static String vagaToNull(String value) {
        return blankToNull(value);
    }

    private static double round4(double value) {
        return BigDecimal.valueOf(value).setScale(4, RoundingMode.HALF_UP).doubleValue();
    }
}
