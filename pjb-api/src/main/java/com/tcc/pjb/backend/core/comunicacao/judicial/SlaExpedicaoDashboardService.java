package com.tcc.pjb.backend.core.comunicacao.judicial;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.comunicacao.institucional.gate.domain.InstitutionalGateState;
import com.tcc.pjb.backend.core.comunicacao.institucional.gate.infrastructure.InstitutionalGateStateRepository;
import com.tcc.pjb.backend.core.comunicacao.institucional.inbox.domain.InstitutionalInboxItem;
import com.tcc.pjb.backend.core.comunicacao.institucional.inbox.infrastructure.InstitutionalInboxStateRepository;
import com.tcc.pjb.backend.core.comunicacao.judicial.state.ComunicacaoJudicialStateStore;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import com.tcc.pjb.backend.platform.cluster.PjbClusterSingletonTask;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SlaExpedicaoDashboardService {

    private static final Logger log = LoggerFactory.getLogger(SlaExpedicaoDashboardService.class);
    private static final String RESOURCE_TYPE = "SLA_DASHBOARD";
    private static final String DOMAIN_BENCHMARK = "SLA_BENCHMARK";
    private static final String DOMAIN_PAINEL = "SLA_PAINEL";
    private static final Duration PANEL_CACHE_TTL = Duration.ofSeconds(30);
    private static final Duration BENCHMARK_CACHE_TTL = Duration.ofMinutes(5);

    public record MetricaSla(
            String dimensao,
            String valor,
            long totalExpedicoes,
            long totalEntregues,
            long totalPresumidas,
            long totalFrustradas,
            double taxaEntregaReal,
            double taxaEntregaTotal,
            long tempoMedioEntregaHoras,
            long tempoMedianoEntregaHoras,
            long tempoP95EntregaHoras,
            double benchmarkNacionalHoras,
            boolean acimaDoSla,
            Instant calculadaEm
    ) {
    }

    public record MetricaSlaInstitucional(
            String dimensao,
            String valor,
            long totalItens,
            long totalCientificadas,
            long totalCumpridas,
            long totalBloqueadas,
            double taxaCumprimento,
            long tempoMedioCienciaHoras,
            long tempoMedioCumprimentoHoras,
            double benchmarkNacionalHoras,
            boolean acimaDoSla,
            Instant calculadaEm
    ) {
    }

    public record PainelSla(
            Map<String, MetricaSla> porModalidade,
            Map<String, MetricaSla> porTipoComunicacao,
            Map<String, MetricaSla> porRamo,
            Map<String, MetricaSla> porComarca,
            Map<String, MetricaSlaInstitucional> porDestinatarioInstitucional,
            Map<String, MetricaSlaInstitucional> porUnidadeInstitucional,
            List<AlertaSla> alertas,
            Instant geradoEm,
            String hashIntegridade
    ) {
    }

    public record AlertaSla(
            String alertaId,
            String dimensao,
            String valorDimensao,
            String descricao,
            double slaAtual,
            double benchmarkNacional,
            double desvioPorcentagem,
            Instant detectadoEm
    ) {
    }

    public record BenchmarkSla(
            String chave,
            double horas,
            Instant atualizadoEm,
            String hashIntegridade
    ) {
    }

    private record PainelCache(PainelSla painel, Instant expiresAt) {
    }

    private final ExpedicaoJudicialRepository expedicaoRepository;
    private final InstitutionalInboxStateRepository institutionalInboxStateRepository;
    private final InstitutionalGateStateRepository institutionalGateStateRepository;
    private final AuditLedgerService auditLedger;
    private final ComunicacaoJudicialStateStore stateStore;
    private final Map<String, Double> benchmarksNacionais = new ConcurrentHashMap<>();
    private final AtomicReference<PainelCache> painelCache = new AtomicReference<>();
    private final AtomicLong benchmarksHydratedAtEpochMilli = new AtomicLong(0L);

    public SlaExpedicaoDashboardService(ExpedicaoJudicialRepository expedicaoRepository,
                                        InstitutionalInboxStateRepository institutionalInboxStateRepository,
                                        InstitutionalGateStateRepository institutionalGateStateRepository,
                                        AuditLedgerService auditLedger,
                                        ComunicacaoJudicialStateStore stateStore) {
        this.expedicaoRepository = Objects.requireNonNull(expedicaoRepository, "expedicaoRepository");
        this.institutionalInboxStateRepository = Objects.requireNonNull(institutionalInboxStateRepository, "institutionalInboxStateRepository");
        this.institutionalGateStateRepository = Objects.requireNonNull(institutionalGateStateRepository, "institutionalGateStateRepository");
        this.auditLedger = Objects.requireNonNull(auditLedger, "auditLedger");
        this.stateStore = Objects.requireNonNull(stateStore, "stateStore");
    }

    @PostConstruct
    void bootstrapBenchmarks() {
        inicializarBenchmarks();
    }

    @Transactional(readOnly = true)
    public PainelSla gerarPainel() {
        PainelCache cache = painelCache.get();
        if (isFresh(cache)) {
            return cache.painel();
        }
        synchronized (painelCache) {
            cache = painelCache.get();
            if (isFresh(cache)) {
                return cache.painel();
            }
            PainelSla persisted = obterPainelPersistidoRecente();
            if (persisted != null) {
                cachePainel(persisted);
                return persisted;
            }
            PainelSla painel = buildPainel();
            cachePainel(painel);
            return painel;
        }
    }

    @Transactional(readOnly = true)
    public MetricaSla metricaPorComarca(String comarca) {
        Objects.requireNonNull(comarca, "comarca");
        List<ExpedicaoJudicial> expedicoes = expedicaoRepository.findByInstanciaExpedidoraIgnoreCase(comarca);
        return calcularMetrica("COMARCA", comarca, expedicoes);
    }

    @Transactional(readOnly = true)
    public MetricaSla metricaPorModalidade(ModalidadeExpedicaoJudicial modalidade) {
        Objects.requireNonNull(modalidade, "modalidade");
        List<ExpedicaoJudicial> expedicoes = expedicaoRepository.findByModalidade(modalidade);
        return calcularMetrica("MODALIDADE", modalidade.name(), expedicoes);
    }

    @Transactional(readOnly = true)
    public Map<String, Double> benchmarksAtuais() {
        ensureBenchmarksHydrated();
        return Map.copyOf(new LinkedHashMap<>(benchmarksNacionais));
    }

    @Transactional
    public void atualizarBenchmark(String dimensao, String valor, double horas) {
        Objects.requireNonNull(dimensao, "dimensao");
        Objects.requireNonNull(valor, "valor");
        if (horas <= 0) {
            throw new IllegalArgumentException("Benchmark deve ser positivo.");
        }
        String chave = chaveBenchmark(dimensao, valor);
        BenchmarkSla benchmark = new BenchmarkSla(chave, horas, Instant.now(), sha256Hex(chave + '|' + horas));
        benchmarksNacionais.put(chave, horas);
        benchmarksHydratedAtEpochMilli.set(System.currentTimeMillis());
        stateStore.save(DOMAIN_BENCHMARK, chave, dimensao.toUpperCase(), benchmark, null, null, null, "ATIVO");
        auditLedger.appendSafely("SLA_BENCHMARK_ATUALIZADO", RESOURCE_TYPE, chave, benchmark.hashIntegridade(), "Benchmark atualizado para %.2fh".formatted(horas));
    }

    @PjbClusterSingletonTask(key = "sla-expedicao-relatorio", ttl = "PT30M")
    @Scheduled(cron = "0 0 6 * * *")
    public void gerarRelatorioNocturno() {
        log.info("[SLA] Gerando relatório nocturno de SLA...");
        PainelSla painel = forceRebuildPainel();
        long alertasCriticos = painel.alertas().stream().filter(a -> a.desvioPorcentagem() > 50.0).count();
        if (alertasCriticos > 0) {
            log.warn("[SLA] {} dimensão(ões) com SLA crítico.", alertasCriticos);
        }
        log.info("[SLA] Relatório nocturno concluído. Alertas críticos: {}", alertasCriticos);
    }

    private PainelSla forceRebuildPainel() {
        synchronized (painelCache) {
            PainelSla painel = buildPainel();
            cachePainel(painel);
            return painel;
        }
    }

    private PainelSla buildPainel() {
        ensureBenchmarksHydrated();
        List<ExpedicaoJudicial> todas = expedicaoRepository.findAll();
        List<InstitutionalInboxItem> inboxItens = institutionalInboxStateRepository.findAll();
        List<InstitutionalGateState> gates = institutionalGateStateRepository.findAll();
        Map<String, MetricaSla> porModalidade = calcularPorDimensao(todas, "MODALIDADE", e -> e.getModalidade() != null ? e.getModalidade().name() : "DESCONHECIDA");
        Map<String, MetricaSla> porTipoComunicacao = calcularPorDimensao(todas, "TIPO", e -> e.getTipoComunicacao() != null ? e.getTipoComunicacao().name() : "DESCONHECIDA");
        Map<String, MetricaSla> porRamo = calcularPorDimensao(todas, "RAMO", e -> e.getRamoDireito() != null ? e.getRamoDireito() : "DESCONHECIDO");
        Map<String, MetricaSla> porComarca = calcularPorDimensao(todas, "COMARCA", e -> e.getInstanciaExpedidora() != null ? e.getInstanciaExpedidora() : "NACIONAL");
        Map<String, MetricaSlaInstitucional> porDestinatarioInstitucional = calcularInstitucionalPorDimensao(
                inboxItens,
                gates,
                "DESTINATARIO_INSTITUCIONAL",
                item -> item.destinatarioKind() != null ? item.destinatarioKind().name() : "DESCONHECIDO"
        );
        Map<String, MetricaSlaInstitucional> porUnidadeInstitucional = calcularInstitucionalPorDimensao(
                inboxItens,
                gates,
                "UNIDADE_INSTITUCIONAL",
                item -> item.unidadeCodigo() != null ? item.unidadeCodigo() : "SEM_UNIDADE"
        );
        List<AlertaSla> alertas = new ArrayList<>();
        alertas.addAll(detectarAlertas(porModalidade));
        alertas.addAll(detectarAlertas(porTipoComunicacao));
        alertas.addAll(detectarAlertas(porRamo));
        alertas.addAll(detectarAlertas(porComarca));
        alertas.addAll(detectarAlertasInstitucionais(porDestinatarioInstitucional));
        alertas.addAll(detectarAlertasInstitucionais(porUnidadeInstitucional));
        Instant now = Instant.now();
        String hash = sha256Hex(porModalidade.size() + "|" + todas.size() + "|" + inboxItens.size() + "|" + now.toEpochMilli());
        PainelSla painel = new PainelSla(
                Map.copyOf(porModalidade),
                Map.copyOf(porTipoComunicacao),
                Map.copyOf(porRamo),
                Map.copyOf(porComarca),
                Map.copyOf(porDestinatarioInstitucional),
                Map.copyOf(porUnidadeInstitucional),
                List.copyOf(alertas),
                now,
                hash
        );
        stateStore.save(DOMAIN_PAINEL, "GLOBAL", hash, painel, null, null, null, "GERADO");
        auditLedger.appendSafely(
                "SLA_PAINEL_GERADO",
                RESOURCE_TYPE,
                "painel:global",
                hash,
                "SLA dashboard gerado. Expedições: %d. Inbox institucional: %d. Alertas: %d".formatted(todas.size(), inboxItens.size(), alertas.size())
        );
        return painel;
    }

    private PainelSla obterPainelPersistidoRecente() {
        PainelSla persisted = stateStore.find(DOMAIN_PAINEL, "GLOBAL", PainelSla.class).orElse(null);
        if (persisted == null || persisted.geradoEm() == null) {
            return null;
        }
        return persisted.geradoEm().isBefore(Instant.now().minus(PANEL_CACHE_TTL)) ? null : persisted;
    }

    private void cachePainel(PainelSla painel) {
        painelCache.set(new PainelCache(painel, Instant.now().plus(PANEL_CACHE_TTL)));
    }

    private boolean isFresh(PainelCache cache) {
        return cache != null && cache.painel() != null && cache.expiresAt() != null && cache.expiresAt().isAfter(Instant.now());
    }

    private void ensureBenchmarksHydrated() {
        long now = System.currentTimeMillis();
        long hydratedAt = benchmarksHydratedAtEpochMilli.get();
        if (!benchmarksNacionais.isEmpty() && hydratedAt > 0L && now - hydratedAt < BENCHMARK_CACHE_TTL.toMillis()) {
            return;
        }
        synchronized (benchmarksNacionais) {
            hydratedAt = benchmarksHydratedAtEpochMilli.get();
            if (!benchmarksNacionais.isEmpty() && hydratedAt > 0L && now - hydratedAt < BENCHMARK_CACHE_TTL.toMillis()) {
                return;
            }
            stateStore.findAll(DOMAIN_BENCHMARK, BenchmarkSla.class)
                    .forEach(benchmark -> benchmarksNacionais.put(benchmark.chave(), benchmark.horas()));
            benchmarksHydratedAtEpochMilli.set(System.currentTimeMillis());
        }
    }

    private Map<String, MetricaSla> calcularPorDimensao(List<ExpedicaoJudicial> todas,
                                                        String nomeDimensao,
                                                        Function<ExpedicaoJudicial, String> extrator) {
        return new HashMap<>(todas.stream()
                .collect(Collectors.groupingBy(extrator))
                .entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> calcularMetrica(nomeDimensao, e.getKey(), e.getValue()))));
    }

    private Map<String, MetricaSlaInstitucional> calcularInstitucionalPorDimensao(List<InstitutionalInboxItem> itens,
                                                                                   List<InstitutionalGateState> gates,
                                                                                   String nomeDimensao,
                                                                                   Function<InstitutionalInboxItem, String> extrator) {
        Map<String, Long> bloqueiosPorChave = new HashMap<>();
        Map<String, InstitutionalInboxItem> itemPorExpedicao = itens.stream().collect(Collectors.toMap(InstitutionalInboxItem::expedicaoUuid, Function.identity(), (a, b) -> a));
        for (InstitutionalGateState gate : gates) {
            if (!gate.bloqueado()) {
                continue;
            }
            InstitutionalInboxItem item = itemPorExpedicao.get(gate.expedicaoUuid());
            if (item == null) {
                continue;
            }
            String chave = extrator.apply(item);
            bloqueiosPorChave.merge(chave, 1L, Long::sum);
        }
        return new HashMap<>(itens.stream()
                .collect(Collectors.groupingBy(extrator))
                .entrySet()
                .stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> calcularMetricaInstitucional(nomeDimensao, entry.getKey(), entry.getValue(), bloqueiosPorChave.getOrDefault(entry.getKey(), 0L))
                )));
    }

    private MetricaSla calcularMetrica(String dimensao, String valor, List<ExpedicaoJudicial> expedicoes) {
        long total = expedicoes.size();
        long entregues = expedicoes.stream().filter(e -> e.getStatus() == ExpedicaoJudicial.StatusExpedicao.ENTREGUE_CONFIRMADA || e.getStatus() == ExpedicaoJudicial.StatusExpedicao.LIDA_CONFIRMADA).count();
        long presumidas = expedicoes.stream().filter(e -> e.getStatus() == ExpedicaoJudicial.StatusExpedicao.PRESUMIDA_ENTREGUE).count();
        long frustradas = expedicoes.stream().filter(e -> e.getStatus() == ExpedicaoJudicial.StatusExpedicao.FRUSTRADA_DEFINITIVA).count();
        double taxaReal = total == 0 ? 0.0 : (double) entregues / total;
        double taxaTotal = total == 0 ? 0.0 : (double) (entregues + presumidas) / total;
        List<Long> tempos = expedicoes.stream()
                .filter(e -> e.getConfirmacaoEntregaEm() != null && e.getExpedidaEm() != null)
                .map(e -> ChronoUnit.HOURS.between(e.getExpedidaEm(), e.getConfirmacaoEntregaEm()))
                .filter(h -> h >= 0)
                .sorted()
                .toList();
        long tempMedio = tempos.isEmpty() ? 0 : (long) tempos.stream().mapToLong(Long::longValue).average().orElse(0);
        long tempMediano = tempos.isEmpty() ? 0 : tempos.get(tempos.size() / 2);
        int idxP95 = tempos.isEmpty() ? 0 : Math.min(tempos.size() - 1, Math.max(0, (int) Math.ceil(tempos.size() * 0.95d) - 1));
        long tempP95 = tempos.isEmpty() ? 0 : tempos.get(idxP95);
        double benchmark = benchmarkPara(dimensao, valor);
        boolean acimaDoSla = tempMedio > benchmark * 1.2;
        return new MetricaSla(dimensao, valor, total, entregues, presumidas, frustradas, taxaReal, taxaTotal, tempMedio, tempMediano, tempP95, benchmark, acimaDoSla, Instant.now());
    }

    private MetricaSlaInstitucional calcularMetricaInstitucional(String dimensao,
                                                                 String valor,
                                                                 List<InstitutionalInboxItem> itens,
                                                                 long totalBloqueadas) {
        long total = itens.size();
        long cientificadas = itens.stream().filter(item -> item.cientificadaEm() != null).count();
        long cumpridas = itens.stream().filter(item -> item.cumpridaEm() != null).count();
        double taxaCumprimento = total == 0 ? 0.0 : (double) cumpridas / total;
        List<Long> temposCiencia = itens.stream()
                .filter(item -> item.disponibilizadaEm() != null && item.cientificadaEm() != null)
                .map(item -> ChronoUnit.HOURS.between(item.disponibilizadaEm(), item.cientificadaEm()))
                .filter(h -> h >= 0)
                .sorted()
                .toList();
        List<Long> temposCumprimento = itens.stream()
                .filter(item -> item.disponibilizadaEm() != null && item.cumpridaEm() != null)
                .map(item -> ChronoUnit.HOURS.between(item.disponibilizadaEm(), item.cumpridaEm()))
                .filter(h -> h >= 0)
                .sorted()
                .toList();
        long tempoMedioCiencia = temposCiencia.isEmpty() ? 0 : (long) temposCiencia.stream().mapToLong(Long::longValue).average().orElse(0);
        long tempoMedioCumprimento = temposCumprimento.isEmpty() ? 0 : (long) temposCumprimento.stream().mapToLong(Long::longValue).average().orElse(0);
        double benchmark = benchmarkPara(dimensao, valor);
        boolean acimaDoSla = tempoMedioCiencia > benchmark * 1.2 || totalBloqueadas > 0 && tempoMedioCumprimento > benchmark * 1.5;
        return new MetricaSlaInstitucional(
                dimensao,
                valor,
                total,
                cientificadas,
                cumpridas,
                totalBloqueadas,
                taxaCumprimento,
                tempoMedioCiencia,
                tempoMedioCumprimento,
                benchmark,
                acimaDoSla,
                Instant.now()
        );
    }

    private List<AlertaSla> detectarAlertas(Map<String, MetricaSla> metricas) {
        return metricas.values().stream().filter(MetricaSla::acimaDoSla).map(m -> {
            double desvio = m.benchmarkNacionalHoras() == 0 ? 0 : ((double) m.tempoMedioEntregaHoras() - m.benchmarkNacionalHoras()) / m.benchmarkNacionalHoras() * 100;
            return new AlertaSla(UUID.randomUUID().toString(), m.dimensao(), m.valor(), "%s '%s' com tempo médio %dh (benchmark: %.0fh). Desvio: +%.1f%%".formatted(m.dimensao(), m.valor(), m.tempoMedioEntregaHoras(), m.benchmarkNacionalHoras(), desvio), m.tempoMedioEntregaHoras(), m.benchmarkNacionalHoras(), desvio, Instant.now());
        }).toList();
    }

    private List<AlertaSla> detectarAlertasInstitucionais(Map<String, MetricaSlaInstitucional> metricas) {
        return metricas.values().stream().filter(MetricaSlaInstitucional::acimaDoSla).map(m -> {
            double desvio = m.benchmarkNacionalHoras() == 0 ? 0 : ((double) m.tempoMedioCienciaHoras() - m.benchmarkNacionalHoras()) / m.benchmarkNacionalHoras() * 100;
            String descricao = "%s '%s' com ciência média %dh, cumprimento médio %dh e %d gate(s) bloqueado(s).".formatted(
                    m.dimensao(),
                    m.valor(),
                    m.tempoMedioCienciaHoras(),
                    m.tempoMedioCumprimentoHoras(),
                    m.totalBloqueadas()
            );
            return new AlertaSla(UUID.randomUUID().toString(), m.dimensao(), m.valor(), descricao, m.tempoMedioCienciaHoras(), m.benchmarkNacionalHoras(), desvio, Instant.now());
        }).toList();
    }

    private void inicializarBenchmarks() {
        registrarBenchmarkPadrao("MODALIDADE", "DIGITAL_GOVBR_PUSH", 24.0);
        registrarBenchmarkPadrao("MODALIDADE", "DIGITAL_DOMICILIO_ELETRONICO_MNI", 72.0);
        registrarBenchmarkPadrao("MODALIDADE", "DIGITAL_SEFAZ_NF_EMAIL", 96.0);
        registrarBenchmarkPadrao("MODALIDADE", "DIGITAL_EMAIL_CERTIFICADO_ICP", 120.0);
        registrarBenchmarkPadrao("MODALIDADE", "PORTAL_EMPRESA_CNPJ", 72.0);
        registrarBenchmarkPadrao("MODALIDADE", "OFICIAL_JUSTICA_ROTA_OTIMIZADA", 168.0);
        registrarBenchmarkPadrao("MODALIDADE", "CORREIO_AR_DIGITAL", 240.0);
        registrarBenchmarkPadrao("MODALIDADE", "EDITAL_DOU_DJE", 480.0);
        registrarBenchmarkPadrao("RAMO", "PENAL", 48.0);
        registrarBenchmarkPadrao("RAMO", "INFANCIA_JUVENTUDE", 24.0);
        registrarBenchmarkPadrao("RAMO", "TRABALHISTA", 96.0);
        registrarBenchmarkPadrao("RAMO", "CIVEL", 120.0);
        registrarBenchmarkPadrao("DESTINATARIO_INSTITUCIONAL", "MINISTERIO_PUBLICO", 24.0);
        registrarBenchmarkPadrao("DESTINATARIO_INSTITUCIONAL", "DEFENSORIA_PUBLICA_ESTADUAL", 48.0);
        registrarBenchmarkPadrao("DESTINATARIO_INSTITUCIONAL", "PROCURADORIA_ESTADO", 72.0);
        registrarBenchmarkPadrao("DESTINATARIO_INSTITUCIONAL", "UNIDADE_PRISIONAL", 12.0);
        ensureBenchmarksHydrated();
    }

    private void registrarBenchmarkPadrao(String dimensao, String valor, double horas) {
        String chave = chaveBenchmark(dimensao, valor);
        benchmarksNacionais.putIfAbsent(chave, horas);
        if (!stateStore.exists(DOMAIN_BENCHMARK, chave)) {
            BenchmarkSla benchmark = new BenchmarkSla(chave, horas, Instant.now(), sha256Hex(chave + '|' + horas));
            stateStore.save(DOMAIN_BENCHMARK, chave, dimensao.toUpperCase(), benchmark, null, null, null, "ATIVO");
        }
    }


    private double benchmarkPara(String dimensao, String valor) {
        ensureBenchmarksHydrated();
        return benchmarksNacionais.getOrDefault(chaveBenchmark(dimensao, valor), 72.0);
    }

    private String chaveBenchmark(String dimensao, String valor) {
        return dimensao.toUpperCase() + ":" + valor.toUpperCase();
    }

    private static String sha256Hex(String raw) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            return Integer.toHexString(Objects.hashCode(raw));
        }
    }
}
