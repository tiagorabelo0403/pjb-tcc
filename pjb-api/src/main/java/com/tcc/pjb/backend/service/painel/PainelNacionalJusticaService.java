package com.tcc.pjb.backend.service.painel;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.model.entity.Jurisdicao;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.federalismo.NoFederacaoJudicial;
import com.tcc.pjb.backend.model.entity.painel.NivelAlertaPrazoNacional;
import com.tcc.pjb.backend.model.entity.painel.TipoEventoPainelNacional;
import com.tcc.pjb.backend.model.entity.painel.PainelAlertaPrazo;
import com.tcc.pjb.backend.model.entity.painel.PainelMateriaMetrica;
import com.tcc.pjb.backend.model.entity.painel.PainelSerieTemporalDiaria;
import com.tcc.pjb.backend.model.entity.painel.PainelTribunalMetrica;
import com.tcc.pjb.backend.model.repository.NoFederacaoJudicialRepository;
import com.tcc.pjb.backend.model.repository.PainelAlertaPrazoRepository;
import com.tcc.pjb.backend.model.repository.PainelMateriaMetricaRepository;
import com.tcc.pjb.backend.model.repository.PainelSerieTemporalDiariaRepository;
import com.tcc.pjb.backend.model.repository.PainelTribunalMetricaRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.service.ajuizamento.federal.FederalismoJudicialEngine;
import com.tcc.pjb.backend.service.outbox.OutboxPublisher;

@Service
public class PainelNacionalJusticaService {

    private static final Duration SNAPSHOT_CACHE_TTL = Duration.ofSeconds(15);

    public static final String TOPIC_PAINEL_NACIONAL = "pjb.nacional.painel.justica.v1";
    public static final String EVT_PAINEL_ATUALIZADO = "pjb.painel.nacional.atualizado";
    public static final String EVT_ALERTA_PRAZO_CRITICO = "pjb.nacional.alertas.prazo.critico.v1";

    private final PainelTribunalMetricaRepository tribunalRepository;
    private final PainelMateriaMetricaRepository materiaRepository;
    private final PainelAlertaPrazoRepository alertaRepository;
    private final PainelSerieTemporalDiariaRepository serieRepository;
    private final ProcessoRepository processoRepository;
    private final NoFederacaoJudicialRepository noFederacaoRepository;
    private final FederalismoJudicialEngine federalismoJudicialEngine;
    private final AuditLedgerService auditLedgerService;
    private final OutboxPublisher outboxPublisher;
    private final ObjectMapper objectMapper;
    private final AtomicReference<CachedSnapshot> snapshotCache = new AtomicReference<>();

    public PainelNacionalJusticaService(PainelTribunalMetricaRepository tribunalRepository,
                                        PainelMateriaMetricaRepository materiaRepository,
                                        PainelAlertaPrazoRepository alertaRepository,
                                        PainelSerieTemporalDiariaRepository serieRepository,
                                        ProcessoRepository processoRepository,
                                        NoFederacaoJudicialRepository noFederacaoRepository,
                                        FederalismoJudicialEngine federalismoJudicialEngine,
                                        AuditLedgerService auditLedgerService,
                                        OutboxPublisher outboxPublisher,
                                        ObjectMapper objectMapper) {
        this.tribunalRepository = Objects.requireNonNull(tribunalRepository);
        this.materiaRepository = Objects.requireNonNull(materiaRepository);
        this.alertaRepository = Objects.requireNonNull(alertaRepository);
        this.serieRepository = Objects.requireNonNull(serieRepository);
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.noFederacaoRepository = Objects.requireNonNull(noFederacaoRepository);
        this.federalismoJudicialEngine = Objects.requireNonNull(federalismoJudicialEngine);
        this.auditLedgerService = Objects.requireNonNull(auditLedgerService);
        this.outboxPublisher = Objects.requireNonNull(outboxPublisher);
        this.objectMapper = Objects.requireNonNull(objectMapper);
    }

    public record SnapshotNacional(
            long totalProcessosAtivos,
            long ajuizadosHoje,
            long ajuizadosSemana,
            long ajuizadosMes,
            long sentenciadosMes,
            long arquivadosMes,
            long acordosMes,
            double taxaConciliacaoPct,
            double tempoMedioResolucaoDias,
            long processosComPrazoExcedido,
            int tribunaisOnline,
            int totalTribunais,
            List<MetricaTribunal> rankingCongestionamento,
            List<MetricaMateria> topMaterias,
            List<AlertaPrazo> alertasPrazo,
            Instant geradoEm
    ) {
    }

    public record MetricaTribunal(
            String tribunalCodigo,
            String tribunalNome,
            String uf,
            long processosAtivos,
            long ajuizadosHoje,
            long ajuizadosSemana,
            long ajuizadosMes,
            long sentenciadosMes,
            long arquivadosMes,
            long acordosMes,
            double indiceCongestionamento,
            double tempoMedioResolucaoDias,
            double taxaConciliacaoPct,
            long processosComPrazoExcedido,
            double disponibilidadeFederativa,
            String classificacaoDesempenho,
            Instant atualizadoEm
    ) {
        public static String classificar(double congestionamento, double tempoMedio, long prazoExcedido, double disponibilidadeFederativa) {
            if (congestionamento < 0.50d && tempoMedio < 180d && prazoExcedido < 50L && disponibilidadeFederativa >= 0.90d) {
                return "EXCELENTE";
            }
            if (congestionamento < 0.70d && tempoMedio < 365d && prazoExcedido < 200L && disponibilidadeFederativa >= 0.70d) {
                return "BOM";
            }
            if (congestionamento < 0.85d && tempoMedio < 730d && disponibilidadeFederativa >= 0.45d) {
                return "REGULAR";
            }
            return "CRITICO";
        }
    }

    public record MetricaMateria(
            String ramoDireito,
            String assuntoTpu,
            long totalProcessos,
            double tempoMedioResolucaoDias,
            double taxaPrescricaoPct,
            double taxaConciliacaoPct,
            String tribunalMaisAtivo
    ) {
    }

    public record AlertaPrazo(
            String nupn,
            String tribunalCodigo,
            String ramoDireito,
            long diasEmAndamento,
            long prazoRazoavelDias,
            long diasExcedidos,
            String fase,
            NivelAlerta nivel
    ) {
        public static AlertaPrazo criar(String nupn,
                                        String tribunalCodigo,
                                        String ramoDireito,
                                        long diasEmAndamento,
                                        long prazoRazoavelDias,
                                        String fase) {
            long excedidos = Math.max(0L, diasEmAndamento - prazoRazoavelDias);
            NivelAlerta nivel = excedidos > 1825L ? NivelAlerta.CRITICO
                    : excedidos > 730L ? NivelAlerta.ALTO
                    : excedidos > 365L ? NivelAlerta.MEDIO
                    : NivelAlerta.BAIXO;
            return new AlertaPrazo(nupn, tribunalCodigo, ramoDireito, diasEmAndamento, prazoRazoavelDias, excedidos, fase, nivel);
        }
    }

    public enum NivelAlerta {
        BAIXO,
        MEDIO,
        ALTO,
        CRITICO
    }

    public record PontoSerieTemporalDiaria(
            LocalDate data,
            String tribunalCodigo,
            String ramoDireito,
            long ajuizados,
            long sentenciados,
            long arquivados,
            long acordos,
            double tempoMedioNovos
    ) {
    }

    public record EventoPainelCommand(
            String eventKey,
            Long processoId,
            String numeroProcesso,
            String tribunalCodigo,
            String tribunalNome,
            String uf,
            String ramoDireito,
            String assuntoTpu,
            String classeTpu,
            TipoEventoPainelNacional tipoEvento,
            Long diasDuracao,
            boolean porAcordo,
            boolean sigiloso,
            Instant ocorridoEm
    ) {
    }

    @Transactional
    public void onProcessoAjuizado(Processo processo) {
        Objects.requireNonNull(processo, "processo");
        String tribunalCodigo = resolveTribunalCodigo(processo);
        String tribunalNome = resolveTribunalNome(tribunalCodigo, processo.getJurisdicao());
        String uf = resolveUf(processo);
        String ramo = resolveRamo(processo);
        String assunto = resolveAssunto(processo);
        LocalDate referencia = resolveDataReferencia(processo.getDataCriacao());

        PainelTribunalMetrica tribunal = tribunalRepository.findByCodigoTribunal(tribunalCodigo)
                .orElseGet(() -> new PainelTribunalMetrica(tribunalCodigo, tribunalNome, uf));
        tribunal.setTribunalNome(tribunalNome);
        tribunal.setUf(uf);
        tribunal.setProcessosAtivos(tribunal.getProcessosAtivos() + 1L);
        tribunal.setAjuizadosHoje(tribunal.getAjuizadosHoje() + 1L);
        tribunal.setAjuizadosSemana(tribunal.getAjuizadosSemana() + 1L);
        tribunal.setAjuizadosMes(tribunal.getAjuizadosMes() + 1L);
        tribunal.setIndiceCongestionamento(decimal(calculateCongestionamento(tribunal.getProcessosAtivos(), tribunal.getArquivadosMes() + tribunal.getSentenciadosMes())));
        tribunal.setClassificacaoDesempenho(classificacao(tribunal, disponibilidadeFederativa(tribunalCodigo)));
        tribunal.setUltimaOcorrenciaEm(Instant.now());
        tribunalRepository.save(tribunal);

        PainelMateriaMetrica materia = materiaRepository.findByChaveMetrica(chaveMateria(ramo, assunto))
                .orElseGet(() -> new PainelMateriaMetrica(chaveMateria(ramo, assunto), ramo, assunto));
        materia.setTotalProcessos(materia.getTotalProcessos() + 1L);
        materia.setTribunalMaisAtivo(resolveTribunalMaisAtivo(materia.getDistribuicaoTribunaisJson(), tribunalCodigo));
        materia.setDistribuicaoTribunaisJson(updateDistribuicaoTribunais(materia.getDistribuicaoTribunaisJson(), tribunalCodigo, 1));
        materiaRepository.save(materia);

        upsertSerie(referencia, tribunalCodigo, ramo, serie -> serie.setAjuizados(serie.getAjuizados() + 1L));
        upsertSerie(referencia, null, null, serie -> serie.setAjuizados(serie.getAjuizados() + 1L));

        auditLedgerService.appendSafely("PAINEL_PROCESSO_AJUIZADO", "Processo", String.valueOf(processo.getId()), processo.getNumeroUnificado(), "Atualizacao de painel nacional");
        publicarAtualizacao("AJUIZADO", processo, tribunalCodigo, ramo, assunto);
        evictSnapshotCache();
    }

    @Transactional
    public void onProcessoEncerrado(Processo processo, boolean porAcordo) {
        Objects.requireNonNull(processo, "processo");
        String tribunalCodigo = resolveTribunalCodigo(processo);
        String ramo = resolveRamo(processo);
        String assunto = resolveAssunto(processo);
        LocalDate referencia = LocalDate.now();
        long diasDuracao = calcularDiasDuracao(processo);

        PainelTribunalMetrica tribunal = tribunalRepository.findByCodigoTribunal(tribunalCodigo)
                .orElseGet(() -> new PainelTribunalMetrica(tribunalCodigo, resolveTribunalNome(tribunalCodigo, processo.getJurisdicao()), resolveUf(processo)));
        tribunal.setProcessosAtivos(Math.max(0L, tribunal.getProcessosAtivos() - 1L));
        tribunal.setArquivadosMes(tribunal.getArquivadosMes() + 1L);
        tribunal.setSentenciadosMes(tribunal.getSentenciadosMes() + 1L);
        if (porAcordo) {
            tribunal.setAcordosMes(tribunal.getAcordosMes() + 1L);
        }
        tribunal.setTempoMedioResolucaoDias(movingAverage(tribunal.getTempoMedioResolucaoDias(), days(diasDuracao), new BigDecimal("0.08")));
        tribunal.setIndiceCongestionamento(decimal(calculateCongestionamento(tribunal.getProcessosAtivos(), tribunal.getArquivadosMes() + tribunal.getSentenciadosMes())));
        tribunal.setTaxaConciliacaoPct(calcularTaxaConciliacao(tribunal.getAcordosMes(), tribunal.getArquivadosMes()));
        tribunal.setClassificacaoDesempenho(classificacao(tribunal, disponibilidadeFederativa(tribunalCodigo)));
        tribunal.setUltimaOcorrenciaEm(Instant.now());
        tribunalRepository.save(tribunal);

        PainelMateriaMetrica materia = materiaRepository.findByChaveMetrica(chaveMateria(ramo, assunto))
                .orElseGet(() -> new PainelMateriaMetrica(chaveMateria(ramo, assunto), ramo, assunto));
        materia.setTempoMedioResolucaoDias(movingAverage(materia.getTempoMedioResolucaoDias(), days(diasDuracao), new BigDecimal("0.06")));
        if (porAcordo) {
            materia.setTaxaConciliacaoPct(calcularTaxaConciliacao(materia.getTaxaConciliacaoPct(), materia.getTotalProcessos()));
        }
        materiaRepository.save(materia);

        upsertSerie(referencia, tribunalCodigo, ramo, serie -> {
            serie.setSentenciados(serie.getSentenciados() + 1L);
            serie.setArquivados(serie.getArquivados() + 1L);
            if (porAcordo) {
                serie.setAcordos(serie.getAcordos() + 1L);
            }
            serie.setTempoMedioNovos(movingAverage(serie.getTempoMedioNovos(), days(diasDuracao), new BigDecimal("0.10")));
        });
        upsertSerie(referencia, null, null, serie -> {
            serie.setSentenciados(serie.getSentenciados() + 1L);
            serie.setArquivados(serie.getArquivados() + 1L);
            if (porAcordo) {
                serie.setAcordos(serie.getAcordos() + 1L);
            }
            serie.setTempoMedioNovos(movingAverage(serie.getTempoMedioNovos(), days(diasDuracao), new BigDecimal("0.10")));
        });

        auditLedgerService.appendSafely("PAINEL_PROCESSO_ENCERRADO", "Processo", String.valueOf(processo.getId()), processo.getNumeroUnificado(), "Atualizacao de encerramento no painel nacional");
        publicarAtualizacao(porAcordo ? "ENCERRADO_ACORDO" : "ENCERRADO", processo, tribunalCodigo, ramo, assunto);
        evictSnapshotCache();
    }

    @Transactional
    public void registrarAlertaPrazo(AlertaPrazo alerta) {
        Objects.requireNonNull(alerta, "alerta");
        if (alerta.nupn() == null || alerta.nupn().isBlank()) {
            throw new IllegalArgumentException("NUPN obrigatorio");
        }
        PainelAlertaPrazo entity = alertaRepository.findByNupn(alerta.nupn()).orElseGet(PainelAlertaPrazo::new);
        entity.setNupn(alerta.nupn());
        entity.setTribunalCodigo(normalizeUpper(alerta.tribunalCodigo()));
        entity.setRamoDireito(normalizeUpper(alerta.ramoDireito()));
        entity.setDiasEmAndamento(alerta.diasEmAndamento());
        entity.setPrazoRazoavelDias(alerta.prazoRazoavelDias());
        entity.setDiasExcedidos(Math.max(0L, alerta.diasExcedidos()));
        entity.setFase(alerta.fase());
        entity.setNivel(alerta.nivel().name());
        entity.setAtivo(alerta.diasExcedidos() > 0L);
        PainelAlertaPrazo salvo = alertaRepository.save(entity);
        tribunalRepository.findByCodigoTribunal(salvo.getTribunalCodigo()).ifPresent(tribunal -> {
            long ativos = alertaRepository.findAllByAtivoTrueOrderByDiasExcedidosDesc().stream()
                    .filter(item -> Objects.equals(item.getTribunalCodigo(), salvo.getTribunalCodigo()))
                    .count();
            tribunal.setProcessosComPrazoExcedido(ativos);
            tribunal.setClassificacaoDesempenho(classificacao(tribunal, disponibilidadeFederativa(tribunal.getCodigoTribunal())));
            tribunalRepository.save(tribunal);
        });
        auditLedgerService.appendSafely("PAINEL_ALERTA_PRAZO", "PainelAlertaPrazo", salvo.getId().toString(), salvo.getNupn(), "Atualizacao de prazo razoavel excedido");
        if (alerta.nivel() == NivelAlerta.CRITICO) {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("nupn", alerta.nupn());
            payload.put("tribunalCodigo", alerta.tribunalCodigo());
            payload.put("ramoDireito", alerta.ramoDireito());
            payload.put("diasExcedidos", alerta.diasExcedidos());
            payload.put("fase", alerta.fase());
            payload.put("nivel", alerta.nivel().name());
            payload.put("at", Instant.now().toString());
            outboxPublisher.enqueue(
                    "painel:alerta:" + alerta.nupn(),
                    EVT_ALERTA_PRAZO_CRITICO,
                    payload,
                    Map.of("source", "painel_nacional"),
                    "painelAlertaCritico:" + alerta.nupn(),
                    "PainelAlertaPrazo",
                    alerta.nupn()
            );
        }
        evictSnapshotCache();
    }

    @Transactional(readOnly = true)
    public SnapshotNacional gerarSnapshot() {
        CachedSnapshot cached = snapshotCache.get();
        if (isFresh(cached)) {
            return cached.snapshot();
        }
        List<PainelTribunalMetrica> metrics = tribunalRepository.findAll();
        List<MetricaTribunal> ranking = tribunalRepository.findTop10ByOrderByIndiceCongestionamentoDesc().stream()
                .map(this::toView)
                .toList();
        List<MetricaMateria> topMaterias = materiaRepository.findTop10ByOrderByTotalProcessosDesc().stream()
                .map(this::toView)
                .toList();
        List<AlertaPrazo> alertas = alertaRepository.findAllByAtivoTrueOrderByDiasExcedidosDesc().stream()
                .limit(100)
                .map(this::toView)
                .toList();

        long totalAtivos = metrics.stream().mapToLong(PainelTribunalMetrica::getProcessosAtivos).sum();
        long ajuizadosHoje = metrics.stream().mapToLong(PainelTribunalMetrica::getAjuizadosHoje).sum();
        long ajuizadosSemana = metrics.stream().mapToLong(PainelTribunalMetrica::getAjuizadosSemana).sum();
        long ajuizadosMes = metrics.stream().mapToLong(PainelTribunalMetrica::getAjuizadosMes).sum();
        long sentenciadosMes = metrics.stream().mapToLong(PainelTribunalMetrica::getSentenciadosMes).sum();
        long arquivadosMes = metrics.stream().mapToLong(PainelTribunalMetrica::getArquivadosMes).sum();
        long acordosMes = metrics.stream().mapToLong(PainelTribunalMetrica::getAcordosMes).sum();
        double taxaConcil = calculateTaxaConciliacaoPct(acordosMes, arquivadosMes);
        double tempoMedio = metrics.stream()
                .map(PainelTribunalMetrica::getTempoMedioResolucaoDias)
                .filter(Objects::nonNull)
                .mapToDouble(BigDecimal::doubleValue)
                .average()
                .orElse(0.0d);
        FederalismoJudicialEngine.FederacaoHealth health = federalismoJudicialEngine.healthFederacao();

        SnapshotNacional snapshot = new SnapshotNacional(
                totalAtivos,
                ajuizadosHoje,
                ajuizadosSemana,
                ajuizadosMes,
                sentenciadosMes,
                arquivadosMes,
                acordosMes,
                taxaConcil,
                tempoMedio,
                alertaRepository.countByAtivoTrue(),
                Math.toIntExact(health.nosOnline()),
                Math.toIntExact(health.totalNos()),
                ranking,
                topMaterias,
                alertas,
                Instant.now()
        );
        snapshotCache.set(new CachedSnapshot(snapshot, Instant.now().plus(SNAPSHOT_CACHE_TTL)));
        return snapshot;
    }

    @Transactional(readOnly = true)
    public Optional<MetricaTribunal> metricasTribunal(String codigoTribunal) {
        return tribunalRepository.findByCodigoTribunal(normalizeUpper(codigoTribunal)).map(this::toView);
    }

    @Transactional(readOnly = true)
    public List<PontoSerieTemporalDiaria> serieTemporal(int dias, String tribunalCodigo) {
        LocalDate limite = LocalDate.now().minusDays(Math.max(0, dias));
        String tribunal = normalizeUpper(tribunalCodigo);
        return serieRepository.findAllByDataReferenciaGreaterThanEqualOrderByDataReferenciaAsc(limite).stream()
                .filter(item -> tribunal == null || Objects.equals(item.getTribunalCodigo(), tribunal))
                .sorted(Comparator.comparing(PainelSerieTemporalDiaria::getDataReferencia).thenComparing(item -> Objects.toString(item.getTribunalCodigo(), "")))
                .map(this::toView)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AlertaPrazo> alertasPrazo(NivelAlerta nivelMinimo, String tribunalCodigo) {
        NivelAlerta filtro = nivelMinimo == null ? NivelAlerta.BAIXO : nivelMinimo;
        String tribunal = normalizeUpper(tribunalCodigo);
        return alertaRepository.findAllByAtivoTrueOrderByDiasExcedidosDesc().stream()
                .map(this::toView)
                .filter(alerta -> alerta.nivel().ordinal() >= filtro.ordinal())
                .filter(alerta -> tribunal == null || tribunal.equals(normalizeUpper(alerta.tribunalCodigo())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AlertaPrazo> alertasPrazo(NivelAlertaPrazoNacional nivelMinimo, String tribunalCodigo) {
        NivelAlerta converted = nivelMinimo == null ? NivelAlerta.BAIXO : NivelAlerta.valueOf(nivelMinimo.name());
        return alertasPrazo(converted, tribunalCodigo);
    }

    @Transactional
    public void registrarEvento(EventoPainelCommand command) {
        Objects.requireNonNull(command, "command");
        TipoEventoPainelNacional tipo = command.tipoEvento() == null ? TipoEventoPainelNacional.AJUIZADO : command.tipoEvento();
        if (tipo == TipoEventoPainelNacional.ALERTA_PRAZO) {
            long diasDuracao = command.diasDuracao() == null ? 0L : Math.max(0L, command.diasDuracao());
            registrarAlertaPrazo(AlertaPrazo.criar(
                    command.numeroProcesso() == null ? Objects.requireNonNullElse(command.eventKey(), "PAINEL") : command.numeroProcesso(),
                    command.tribunalCodigo(),
                    command.ramoDireito(),
                    diasDuracao,
                    prazoRazoavelPorRamo(command.ramoDireito()),
                    command.classeTpu()
            ));
            return;
        }

        Processo processo = command.processoId() != null ? processoRepository.findById(command.processoId()).orElse(null) : null;
        if (processo == null) {
            registrarEventoAjuizado(command.tribunalCodigo(), command.ramoDireito(), command.ocorridoEm());
            return;
        }

        switch (tipo) {
            case AJUIZADO -> onProcessoAjuizado(processo);
            case ACORDO -> onProcessoEncerrado(processo, true);
            case ARQUIVADO, SENTENCIADO -> onProcessoEncerrado(processo, command.porAcordo());
            case ALERTA_PRAZO -> throw new IllegalStateException("Tipo de evento nao suportado");
        }
    }

    @Transactional
    public void registrarEventoAjuizado(String tribunalCodigo, String ramoDireito, Instant ocorridoEm) {
        String tribunal = normalizeUpper(tribunalCodigo) == null ? "PJB" : normalizeUpper(tribunalCodigo);
        String ramo = normalizeUpper(ramoDireito) == null ? "NAO_INFORMADO" : normalizeUpper(ramoDireito);
        LocalDate referencia = ocorridoEm == null ? LocalDate.now() : LocalDateTime.ofInstant(ocorridoEm, java.time.ZoneOffset.UTC).toLocalDate();
        PainelTribunalMetrica item = tribunalRepository.findByCodigoTribunal(tribunal)
                .orElseGet(() -> new PainelTribunalMetrica(tribunal, tribunal, noFederacaoRepository.findByCodigoTribunal(tribunal).map(NoFederacaoJudicial::getUf).orElse("N/D")));
        item.setProcessosAtivos(item.getProcessosAtivos() + 1L);
        item.setAjuizadosHoje(item.getAjuizadosHoje() + 1L);
        item.setAjuizadosSemana(item.getAjuizadosSemana() + 1L);
        item.setAjuizadosMes(item.getAjuizadosMes() + 1L);
        item.setIndiceCongestionamento(decimal(calculateCongestionamento(item.getProcessosAtivos(), item.getArquivadosMes() + item.getSentenciadosMes())));
        item.setClassificacaoDesempenho(classificacao(item, disponibilidadeFederativa(tribunal)));
        item.setUltimaOcorrenciaEm(Objects.requireNonNullElse(ocorridoEm, Instant.now()));
        tribunalRepository.save(item);
        upsertSerie(referencia, tribunal, ramo, serie -> serie.setAjuizados(serie.getAjuizados() + 1L));
        upsertSerie(referencia, null, null, serie -> serie.setAjuizados(serie.getAjuizados() + 1L));
        evictSnapshotCache();
    }

    private boolean isFresh(CachedSnapshot cache) {
        return cache != null && cache.expiresAt() != null && cache.expiresAt().isAfter(Instant.now());
    }

    private void evictSnapshotCache() {
        snapshotCache.set(null);
    }

    public MetricaTribunal toView(PainelTribunalMetrica item) {
        double disponibilidade = disponibilidadeFederativa(item.getCodigoTribunal());
        return new MetricaTribunal(
                item.getCodigoTribunal(),
                item.getTribunalNome(),
                item.getUf(),
                item.getProcessosAtivos(),
                item.getAjuizadosHoje(),
                item.getAjuizadosSemana(),
                item.getAjuizadosMes(),
                item.getSentenciadosMes(),
                item.getArquivadosMes(),
                item.getAcordosMes(),
                value(item.getIndiceCongestionamento()),
                value(item.getTempoMedioResolucaoDias()),
                value(item.getTaxaConciliacaoPct()),
                item.getProcessosComPrazoExcedido(),
                disponibilidade,
                classificacao(item, disponibilidade),
                item.getAtualizadoEm()
        );
    }

    private MetricaMateria toView(PainelMateriaMetrica item) {
        return new MetricaMateria(
                item.getRamoDireito(),
                item.getAssuntoTpu(),
                item.getTotalProcessos(),
                value(item.getTempoMedioResolucaoDias()),
                value(item.getTaxaPrescricaoPct()),
                value(item.getTaxaConciliacaoPct()),
                item.getTribunalMaisAtivo()
        );
    }

    private AlertaPrazo toView(PainelAlertaPrazo item) {
        return new AlertaPrazo(
                item.getNupn(),
                item.getTribunalCodigo(),
                item.getRamoDireito(),
                item.getDiasEmAndamento(),
                item.getPrazoRazoavelDias(),
                item.getDiasExcedidos(),
                item.getFase(),
                NivelAlerta.valueOf(item.getNivel())
        );
    }

    private PontoSerieTemporalDiaria toView(PainelSerieTemporalDiaria item) {
        return new PontoSerieTemporalDiaria(
                item.getDataReferencia(),
                item.getTribunalCodigo(),
                item.getRamoDireito(),
                item.getAjuizados(),
                item.getSentenciados(),
                item.getArquivados(),
                item.getAcordos(),
                value(item.getTempoMedioNovos())
        );
    }

    private void upsertSerie(LocalDate data, String tribunalCodigo, String ramoDireito, java.util.function.Consumer<PainelSerieTemporalDiaria> mutator) {
        String key = keySerie(data, tribunalCodigo, ramoDireito);
        PainelSerieTemporalDiaria serie = serieRepository.findByChaveSerie(key)
                .orElseGet(() -> new PainelSerieTemporalDiaria(key, data, tribunalCodigo, ramoDireito));
        mutator.accept(serie);
        serieRepository.save(serie);
    }

    private String resolveTribunalCodigo(Processo processo) {
        if (processo.getJurisdicao() != null) {
            if (processo.getJurisdicao().getSigla() != null && !processo.getJurisdicao().getSigla().isBlank()) {
                return normalizeUpper(processo.getJurisdicao().getSigla());
            }
            if (processo.getJurisdicao().getCodigo() != null && !processo.getJurisdicao().getCodigo().isBlank()) {
                return normalizeUpper(processo.getJurisdicao().getCodigo());
            }
        }
        if (processo.getTipoJustica() != null) {
            return normalizeUpper(processo.getTipoJustica().name());
        }
        return "PJB";
    }

    private String resolveTribunalNome(String codigoTribunal, Jurisdicao jurisdicao) {
        Optional<NoFederacaoJudicial> no = noFederacaoRepository.findByCodigoTribunal(normalizeUpper(codigoTribunal));
        if (no.isPresent() && no.get().getNome() != null) {
            return no.get().getNome();
        }
        if (jurisdicao != null && jurisdicao.getNome() != null && !jurisdicao.getNome().isBlank()) {
            return jurisdicao.getNome();
        }
        return codigoTribunal;
    }

    private String resolveUf(Processo processo) {
        if (processo.getJurisdicao() != null && processo.getJurisdicao().getEstado() != null && !processo.getJurisdicao().getEstado().isBlank()) {
            return normalizeUpper(processo.getJurisdicao().getEstado());
        }
        String tribunalCodigo = resolveTribunalCodigo(processo);
        return noFederacaoRepository.findByCodigoTribunal(tribunalCodigo).map(NoFederacaoJudicial::getUf).map(this::normalizeUpper).orElse("N/D");
    }

    private String resolveRamo(Processo processo) {
        return processo.getRamoDireito() != null ? normalizeUpper(processo.getRamoDireito().name()) : "NAO_INFORMADO";
    }

    private String resolveAssunto(Processo processo) {
        String assunto = processo.getAssunto();
        if (assunto == null || assunto.isBlank()) {
            assunto = processo.getClasseProcessual();
        }
        if (assunto == null || assunto.isBlank()) {
            assunto = "INDETERMINADO";
        }
        return assunto.trim();
    }

    private LocalDate resolveDataReferencia(LocalDateTime localDateTime) {
        return localDateTime == null ? LocalDate.now() : localDateTime.toLocalDate();
    }

    private long calcularDiasDuracao(Processo processo) {
        LocalDateTime inicio = processo.getDataCriacao();
        LocalDateTime fim = processo.getDataUltimaMovimentacao() != null ? processo.getDataUltimaMovimentacao() : LocalDateTime.now();
        if (inicio == null) {
            return 0L;
        }
        return Math.max(0L, ChronoUnit.DAYS.between(inicio, fim));
    }

    private String chaveMateria(String ramo, String assunto) {
        return normalizeUpper(ramo) + "|" + normalizeText(assunto);
    }

    private String keySerie(LocalDate data, String tribunalCodigo, String ramoDireito) {
        return data + "|" + Objects.toString(normalizeUpper(tribunalCodigo), "NACIONAL") + "|" + Objects.toString(normalizeUpper(ramoDireito), "TODOS");
    }

    private String updateDistribuicaoTribunais(String json, String tribunalCodigo, int increment) {
        Map<String, Long> distribuicao = parseDistribuicao(json);
        String tribunal = normalizeUpper(tribunalCodigo);
        distribuicao.merge(tribunal, (long) increment, Long::sum);
        return toJson(distribuicao);
    }

    private String resolveTribunalMaisAtivo(String json, String fallbackTribunal) {
        Map<String, Long> distribuicao = parseDistribuicao(json);
        if (distribuicao.isEmpty()) {
            return normalizeUpper(fallbackTribunal);
        }
        return distribuicao.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(normalizeUpper(fallbackTribunal));
    }

    private Map<String, Long> parseDistribuicao(String json) {
        if (json == null || json.isBlank()) {
            return new LinkedHashMap<>();
        }
        try {
            return new LinkedHashMap<>(objectMapper.readValue(json, new TypeReference<Map<String, Long>>() {
            }));
        } catch (Exception ex) {
            return new LinkedHashMap<>();
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw new IllegalStateException("painel json", ex);
        }
    }

    private BigDecimal movingAverage(BigDecimal atual, BigDecimal novoValor, BigDecimal alpha) {
        BigDecimal atualNormalizado = atual == null ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) : atual.setScale(2, RoundingMode.HALF_UP);
        BigDecimal novoNormalizado = novoValor == null ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) : novoValor.setScale(2, RoundingMode.HALF_UP);
        BigDecimal pesoAnterior = BigDecimal.ONE.subtract(alpha);
        return atualNormalizado.multiply(pesoAnterior).add(novoNormalizado.multiply(alpha)).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal days(long dias) {
        return BigDecimal.valueOf(Math.max(0L, dias)).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal decimal(double value) {
        return BigDecimal.valueOf(value).setScale(6, RoundingMode.HALF_UP);
    }

    private double value(BigDecimal value) {
        return value == null ? 0.0d : value.doubleValue();
    }

    private double calculateCongestionamento(long processosAtivos, long baixados) {
        long pendentes = Math.max(0L, processosAtivos);
        long base = Math.max(1L, pendentes + Math.max(0L, baixados));
        return Math.min(1.0d, (double) pendentes / (double) base);
    }

    private BigDecimal calcularTaxaConciliacao(long acordos, long encerrados) {
        if (encerrados <= 0L) {
            return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(acordos)
                .multiply(new BigDecimal("100"))
                .divide(BigDecimal.valueOf(encerrados), 4, RoundingMode.HALF_UP);
    }

    private BigDecimal calcularTaxaConciliacao(BigDecimal taxaAtual, long totalProcessos) {
        if (totalProcessos <= 0L) {
            return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        }
        BigDecimal atual = taxaAtual == null ? BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP) : taxaAtual.setScale(4, RoundingMode.HALF_UP);
        BigDecimal incremento = BigDecimal.valueOf(100L).divide(BigDecimal.valueOf(totalProcessos), 4, RoundingMode.HALF_UP);
        BigDecimal total = atual.add(incremento);
        return total.compareTo(new BigDecimal("100")) > 0 ? new BigDecimal("100.0000") : total;
    }

    private double calculateTaxaConciliacaoPct(long acordos, long encerrados) {
        return calcularTaxaConciliacao(acordos, encerrados).doubleValue();
    }

    private double disponibilidadeFederativa(String codigoTribunal) {
        return noFederacaoRepository.findByCodigoTribunal(normalizeUpper(codigoTribunal))
                .map(NoFederacaoJudicial::disponibilidadeFederativa)
                .orElse(1.0d);
    }

    private String classificacao(PainelTribunalMetrica tribunal, double disponibilidade) {
        return MetricaTribunal.classificar(
                value(tribunal.getIndiceCongestionamento()),
                value(tribunal.getTempoMedioResolucaoDias()),
                tribunal.getProcessosComPrazoExcedido(),
                disponibilidade
        );
    }

    private long prazoRazoavelPorRamo(String ramoDireito) {
        String ramo = normalizeUpper(ramoDireito);
        if (ramo == null) {
            return 730L;
        }
        return switch (ramo) {
            case "PENAL" -> 540L;
            case "TRABALHISTA", "TRABALHO" -> 365L;
            case "FAZENDA_PUBLICA", "EXECUCAO_FISCAL" -> 1095L;
            case "FAMILIA" -> 450L;
            default -> 730L;
        };
    }

    private void publicarAtualizacao(String operacao, Processo processo, String tribunalCodigo, String ramoDireito, String assunto) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("operacao", operacao);
        payload.put("processoId", processo.getId());
        payload.put("numeroUnificado", processo.getNumeroUnificado());
        payload.put("tribunalCodigo", tribunalCodigo);
        payload.put("ramoDireito", ramoDireito);
        payload.put("assunto", assunto);
        payload.put("at", Instant.now().toString());
        outboxPublisher.enqueue(
                "painel:nacional:" + tribunalCodigo,
                EVT_PAINEL_ATUALIZADO,
                payload,
                Map.of("source", "painel_nacional"),
                "painelAtualizado:" + operacao + ":" + Objects.toString(processo.getId(), processo.getNumeroUnificado()),
                "Processo",
                processo.getId() != null ? processo.getId().toString() : processo.getNumeroUnificado()
        );
    }

    private String normalizeUpper(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.strip();
        return normalized.isEmpty() ? null : normalized.toUpperCase(Locale.ROOT);
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().replaceAll("\\s+", " ");
        return normalized.isEmpty() ? null : normalized;
    }


    private record CachedSnapshot(SnapshotNacional snapshot, Instant expiresAt) {
    }
}
