package com.tcc.pjb.backend.core.comunicacao.institucional.observability.application;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import com.tcc.pjb.backend.core.comunicacao.institucional.delivery.domain.InstitutionalDeliveryJob;
import com.tcc.pjb.backend.core.comunicacao.institucional.delivery.infrastructure.InstitutionalDeliveryDeadLetterStateRepository;
import com.tcc.pjb.backend.core.comunicacao.institucional.delivery.infrastructure.InstitutionalDeliveryJobStateRepository;
import com.tcc.pjb.backend.core.comunicacao.institucional.gate.domain.InstitutionalGateState;
import com.tcc.pjb.backend.core.comunicacao.institucional.gate.infrastructure.InstitutionalGateStateRepository;
import com.tcc.pjb.backend.core.comunicacao.institucional.inbox.domain.InstitutionalInboxItem;
import com.tcc.pjb.backend.core.comunicacao.institucional.inbox.infrastructure.InstitutionalInboxStateRepository;
import com.tcc.pjb.backend.core.comunicacao.institucional.integration.domain.InstitutionalExternalDispatch;
import com.tcc.pjb.backend.core.comunicacao.institucional.integration.infrastructure.InstitutionalExternalDispatchStateRepository;
import com.tcc.pjb.backend.core.comunicacao.institucional.observability.domain.InstitutionalObservabilityBucket;
import com.tcc.pjb.backend.core.comunicacao.institucional.observability.domain.InstitutionalObservabilityDashboard;
import com.tcc.pjb.backend.model.entity.enums.DestinatarioInstitucionalKind;
import com.tcc.pjb.backend.model.entity.enums.StatusIntegracaoInstitucionalExterna;

@Service
public class InstitutionalCommunicationObservabilityApplicationService {

    private static final int MAX_COUNTERS = 256;
    private static final int MAX_PROVIDER_TAGS = 64;
    private static final int MAX_SCOPED_CACHE_ENTRIES = 192;
    private static final Duration GLOBAL_DASHBOARD_CACHE_TTL = Duration.ofSeconds(30);

    private final InstitutionalDeliveryJobStateRepository jobRepository;
    private final InstitutionalDeliveryDeadLetterStateRepository deadLetterRepository;
    private final InstitutionalExternalDispatchStateRepository externalDispatchRepository;
    private final InstitutionalGateStateRepository gateRepository;
    private final InstitutionalInboxStateRepository inboxRepository;
    private final MeterRegistry meterRegistry;
    private final ConcurrentHashMap<String, Counter> integrationCounters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Boolean> providerTags = new ConcurrentHashMap<>();
    private final AtomicReference<CachedDashboard> globalDashboardCache = new AtomicReference<>();
    private final ConcurrentHashMap<String, CachedDashboard> scopedDashboardCache = new ConcurrentHashMap<>();

    public InstitutionalCommunicationObservabilityApplicationService(InstitutionalDeliveryJobStateRepository jobRepository,
                                                                     InstitutionalDeliveryDeadLetterStateRepository deadLetterRepository,
                                                                     InstitutionalExternalDispatchStateRepository externalDispatchRepository,
                                                                     InstitutionalGateStateRepository gateRepository,
                                                                     InstitutionalInboxStateRepository inboxRepository,
                                                                     MeterRegistry meterRegistry) {
        this.jobRepository = Objects.requireNonNull(jobRepository);
        this.deadLetterRepository = Objects.requireNonNull(deadLetterRepository);
        this.externalDispatchRepository = Objects.requireNonNull(externalDispatchRepository);
        this.gateRepository = Objects.requireNonNull(gateRepository);
        this.inboxRepository = Objects.requireNonNull(inboxRepository);
        this.meterRegistry = Objects.requireNonNull(meterRegistry);
    }

    public InstitutionalExternalDispatch registrarIntegracaoExterna(InstitutionalExternalDispatch dispatch) {
        InstitutionalExternalDispatch saved = externalDispatchRepository.save(dispatch);
        String provider = providerTag(saved.provider());
        String key = saved.channel().name() + '|' + provider + '|' + saved.status().name();
        counter(key, saved.channel().name(), provider, saved.status().name()).increment();
        globalDashboardCache.set(null);
        scopedDashboardCache.clear();
        return saved;
    }

    public InstitutionalObservabilityDashboard dashboard(Long processoId, String uf, DestinatarioInstitucionalKind destinatarioKind) {
        String normalizedUf = normalizeUf(uf);
        boolean global = processoId == null && normalizedUf == null && destinatarioKind == null;
        if (global) {
            CachedDashboard cache = globalDashboardCache.get();
            if (isFresh(cache)) {
                return cache.dashboard();
            }
        } else {
            CachedDashboard cache = scopedDashboardCache.get(cacheKey(processoId, normalizedUf, destinatarioKind));
            if (isFresh(cache)) {
                return cache.dashboard();
            }
        }
        Dataset dataset = loadDataset(processoId, normalizedUf, destinatarioKind);
        List<InstitutionalDeliveryJob> jobs = dataset.jobs();
        List<InstitutionalExternalDispatch> external = dataset.external();
        List<InstitutionalGateState> gates = dataset.gates();
        List<InstitutionalInboxItem> inbox = dataset.inbox();
        long totalDlq = dataset.totalDlq();
        long totalPendentes = jobs.stream().filter(j -> !j.status().isTerminal()).count();
        long totalIntegracoesAceitas = external.stream().filter(d -> d.status() == StatusIntegracaoInstitucionalExterna.ACEITA).count();
        long totalIntegracoesFalha = external.stream().filter(d -> d.status() == StatusIntegracaoInstitucionalExterna.FALHA_TERMINAL || d.status() == StatusIntegracaoInstitucionalExterna.FALHA_TRANSITORIA).count();
        long totalGatesBloqueando = gates.stream().filter(InstitutionalGateState::bloqueando).count();
        long totalInboxPendentes = inbox.stream().filter(i -> !i.status().isTerminal()).count();
        Instant threshold = Instant.now().plusSeconds(6 * 3600L);
        long totalSlaRisco = jobs.stream().filter(j -> !j.status().isTerminal()).filter(j -> j.nextAttemptAt() != null && !j.nextAttemptAt().isAfter(threshold)).count();
        InstitutionalObservabilityDashboard dashboard = new InstitutionalObservabilityDashboard(
                jobs.size(),
                totalPendentes,
                totalDlq,
                external.size(),
                totalIntegracoesAceitas,
                totalIntegracoesFalha,
                totalGatesBloqueando,
                totalInboxPendentes,
                totalSlaRisco,
                aggregate(jobs.stream().collect(Collectors.groupingBy(j -> j.currentChannel() != null ? j.currentChannel().name() : "UNSPECIFIED", Collectors.counting()))),
                aggregate(jobs.stream().collect(Collectors.groupingBy(j -> j.status() != null ? j.status().name() : "UNSPECIFIED", Collectors.counting()))),
                aggregate(external.stream().collect(Collectors.groupingBy(d -> d.destinatarioKind() != null ? d.destinatarioKind().name() : "UNSPECIFIED", Collectors.counting()))),
                Instant.now()
        );
        cacheDashboard(processoId, normalizedUf, destinatarioKind, dashboard, global);
        return dashboard;
    }

    public List<InstitutionalExternalDispatch> listarIntegracoesExternas(Long processoId, String expedicaoUuid) {
        if (expedicaoUuid != null && !expedicaoUuid.isBlank()) {
            return externalDispatchRepository.findByExpedicaoUuid(expedicaoUuid);
        }
        if (processoId != null) {
            return externalDispatchRepository.findByProcessoId(processoId);
        }
        return List.of();
    }

    private Dataset loadDataset(Long processoId, String normalizedUf, DestinatarioInstitucionalKind destinatarioKind) {
        if (processoId != null) {
            List<com.tcc.pjb.backend.core.comunicacao.institucional.delivery.domain.InstitutionalDeadLetterEntry> dlq = deadLetterRepository.findByProcessoId(processoId);
            return new Dataset(
                    jobRepository.findByProcessoId(processoId),
                    externalDispatchRepository.findByProcessoId(processoId),
                    gateRepository.findByProcessoId(processoId),
                    inboxRepository.findByProcessoId(processoId),
                    dlq.size()
            );
        }
        if (normalizedUf != null && destinatarioKind != null) {
            return new Dataset(
                    jobRepository.findByUnidadeCodigoContainingIgnoreCaseAndDestinatarioKind(normalizedUf, destinatarioKind),
                    externalDispatchRepository.findByUnidadeCodigoContainingIgnoreCaseAndDestinatarioKind(normalizedUf, destinatarioKind),
                    gateRepository.findByGateCodeContainingIgnoreCase(normalizedUf),
                    inboxRepository.findByUnidadeCodigoContainingIgnoreCase(normalizedUf),
                    deadLetterRepository.countAll()
            );
        }
        if (normalizedUf != null) {
            return new Dataset(
                    jobRepository.findByUnidadeCodigoContainingIgnoreCase(normalizedUf),
                    externalDispatchRepository.findByUnidadeCodigoContainingIgnoreCase(normalizedUf),
                    gateRepository.findByGateCodeContainingIgnoreCase(normalizedUf),
                    inboxRepository.findByUnidadeCodigoContainingIgnoreCase(normalizedUf),
                    deadLetterRepository.countAll()
            );
        }
        if (destinatarioKind != null) {
            return new Dataset(
                    jobRepository.findByDestinatarioKind(destinatarioKind),
                    externalDispatchRepository.findByDestinatarioKind(destinatarioKind),
                    gateRepository.findAll(),
                    inboxRepository.findAll(),
                    deadLetterRepository.countAll()
            );
        }
        return new Dataset(
                jobRepository.findAll(),
                externalDispatchRepository.findAll(),
                gateRepository.findAll(),
                inboxRepository.findAll(),
                deadLetterRepository.countAll()
        );
    }

    private void cacheDashboard(Long processoId,
                                String normalizedUf,
                                DestinatarioInstitucionalKind destinatarioKind,
                                InstitutionalObservabilityDashboard dashboard,
                                boolean global) {
        CachedDashboard cached = new CachedDashboard(dashboard, Instant.now().plus(GLOBAL_DASHBOARD_CACHE_TTL));
        if (global) {
            globalDashboardCache.set(cached);
            return;
        }
        pruneScopedCache();
        scopedDashboardCache.put(cacheKey(processoId, normalizedUf, destinatarioKind), cached);
    }

    private void pruneScopedCache() {
        Instant now = Instant.now();
        scopedDashboardCache.entrySet().removeIf(entry -> !isFresh(entry.getValue()));
        int overflow = scopedDashboardCache.size() - MAX_SCOPED_CACHE_ENTRIES + 1;
        if (overflow <= 0) {
            return;
        }
        ArrayList<Map.Entry<String, CachedDashboard>> entries = new ArrayList<>(scopedDashboardCache.entrySet());
        entries.sort(Comparator.comparing(entry -> entry.getValue().expiresAt(), Comparator.nullsFirst(Comparator.naturalOrder())));
        for (int i = 0; i < overflow && i < entries.size(); i++) {
            CachedDashboard candidate = entries.get(i).getValue();
            scopedDashboardCache.remove(entries.get(i).getKey(), candidate);
        }
    }

    private Counter counter(String key, String channel, String provider, String status) {
        Counter existing = integrationCounters.get(key);
        if (existing != null) {
            return existing;
        }
        if (integrationCounters.size() >= MAX_COUNTERS) {
            return integrationCounters.computeIfAbsent("OTHER|OTHER|OTHER", ignored -> Counter.builder("pjb.comunicacao.institucional.integracao_externa.total")
                    .tag("canal", "OTHER")
                    .tag("provider", "OTHER")
                    .tag("status", "OTHER")
                    .register(meterRegistry));
        }
        return integrationCounters.computeIfAbsent(key, ignored -> Counter.builder("pjb.comunicacao.institucional.integracao_externa.total")
                .tag("canal", channel)
                .tag("provider", provider)
                .tag("status", status)
                .register(meterRegistry));
    }

    private List<InstitutionalObservabilityBucket> aggregate(Map<String, Long> counts) {
        return counts.entrySet().stream()
                .map(entry -> new InstitutionalObservabilityBucket(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparingLong(InstitutionalObservabilityBucket::count).reversed().thenComparing(InstitutionalObservabilityBucket::key, Comparator.nullsLast(String::compareTo)))
                .limit(20)
                .toList();
    }

    private String providerTag(String value) {
        String normalized = normalize(value);
        if (providerTags.containsKey(normalized)) {
            return normalized;
        }
        if (providerTags.size() >= MAX_PROVIDER_TAGS) {
            return "OTHER";
        }
        providerTags.putIfAbsent(normalized, Boolean.TRUE);
        return providerTags.size() > MAX_PROVIDER_TAGS ? "OTHER" : normalized;
    }

    private boolean isFresh(CachedDashboard cache) {
        return cache != null && cache.dashboard() != null && cache.expiresAt() != null && cache.expiresAt().isAfter(Instant.now());
    }

    private String cacheKey(Long processoId, String normalizedUf, DestinatarioInstitucionalKind destinatarioKind) {
        return String.valueOf(processoId) + '|' + (normalizedUf == null ? "ALL" : normalizedUf) + '|' + (destinatarioKind == null ? "ALL" : destinatarioKind.name());
    }

    private static String normalizeUf(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toUpperCase();
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "UNSPECIFIED";
        }
        String normalized = value.trim().toUpperCase().replaceAll("[^A-Z0-9_.:-]", "_");
        return normalized.length() > 64 ? normalized.substring(0, 64) : normalized;
    }

    private record CachedDashboard(InstitutionalObservabilityDashboard dashboard, Instant expiresAt) {}
    private record Dataset(List<InstitutionalDeliveryJob> jobs,
                           List<InstitutionalExternalDispatch> external,
                           List<InstitutionalGateState> gates,
                           List<InstitutionalInboxItem> inbox,
                           long totalDlq) {}
}
