package com.tcc.pjb.backend.core.comunicacao.institucional.workflow.application;

import com.tcc.pjb.backend.core.comunicacao.institucional.audit.domain.InstitutionalTimelineEvent;
import com.tcc.pjb.backend.core.comunicacao.institucional.audit.domain.InstitutionalTimelineEventType;
import com.tcc.pjb.backend.core.comunicacao.institucional.audit.infrastructure.InstitutionalTimelineStateRepository;
import com.tcc.pjb.backend.core.comunicacao.institucional.delivery.infrastructure.InstitutionalDeliveryJobStateRepository;
import com.tcc.pjb.backend.core.comunicacao.institucional.inbox.domain.InstitutionalInboxItem;
import com.tcc.pjb.backend.core.comunicacao.institucional.inbox.infrastructure.InstitutionalInboxStateRepository;
import com.tcc.pjb.backend.core.comunicacao.institucional.workflow.domain.InstitutionalDelegationAssignment;
import com.tcc.pjb.backend.core.comunicacao.institucional.workflow.domain.InstitutionalDraftManifestation;
import com.tcc.pjb.backend.core.comunicacao.institucional.workflow.domain.InstitutionalFlowAnalyticsBucket;
import com.tcc.pjb.backend.core.comunicacao.institucional.workflow.domain.InstitutionalFlowAnalyticsDashboard;
import com.tcc.pjb.backend.core.comunicacao.institucional.workflow.domain.InstitutionalSemanticTimelineEntry;
import com.tcc.pjb.backend.core.comunicacao.institucional.workflow.infrastructure.InstitutionalDelegationAssignmentStateRepository;
import com.tcc.pjb.backend.core.comunicacao.institucional.workflow.infrastructure.InstitutionalDraftManifestationStateRepository;
import com.tcc.pjb.backend.model.entity.enums.StatusDelegacaoInstitucional;
import com.tcc.pjb.backend.model.entity.enums.StatusMinutaInstitucional;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InstitutionalFlowAnalyticsApplicationService {

    private static final Duration GLOBAL_CACHE_TTL = Duration.ofSeconds(20);
    private static final Duration SCOPED_CACHE_TTL = Duration.ofSeconds(15);
    private static final int MAX_SCOPED_CACHE = 256;

    private final InstitutionalInboxStateRepository inboxRepository;
    private final InstitutionalDeliveryJobStateRepository deliveryJobRepository;
    private final InstitutionalDelegationAssignmentStateRepository delegationRepository;
    private final InstitutionalDraftManifestationStateRepository draftRepository;
    private final InstitutionalTimelineStateRepository timelineRepository;
    private final AtomicReference<CachedDashboard> globalDashboardCache = new AtomicReference<>();
    private final ConcurrentHashMap<ScopedDashboardKey, CachedDashboard> scopedDashboardCache = new ConcurrentHashMap<>();

    public InstitutionalFlowAnalyticsApplicationService(InstitutionalInboxStateRepository inboxRepository,
                                                        InstitutionalDeliveryJobStateRepository deliveryJobRepository,
                                                        InstitutionalDelegationAssignmentStateRepository delegationRepository,
                                                        InstitutionalDraftManifestationStateRepository draftRepository,
                                                        InstitutionalTimelineStateRepository timelineRepository) {
        this.inboxRepository = Objects.requireNonNull(inboxRepository);
        this.deliveryJobRepository = Objects.requireNonNull(deliveryJobRepository);
        this.delegationRepository = Objects.requireNonNull(delegationRepository);
        this.draftRepository = Objects.requireNonNull(draftRepository);
        this.timelineRepository = Objects.requireNonNull(timelineRepository);
    }

    @Transactional(readOnly = true)
    public InstitutionalFlowAnalyticsDashboard dashboard(Long processoId, String expedicaoUuid) {
        boolean global = processoId == null && (expedicaoUuid == null || expedicaoUuid.isBlank());
        if (global) {
            CachedDashboard cache = globalDashboardCache.get();
            if (isFresh(cache)) {
                return cache.dashboard();
            }
        } else {
            ScopedDashboardKey key = new ScopedDashboardKey(processoId, normalizeExpedicaoUuid(expedicaoUuid));
            CachedDashboard cache = scopedDashboardCache.get(key);
            if (isFresh(cache)) {
                return cache.dashboard();
            }
        }
        List<InstitutionalInboxItem> inbox = selectInbox(processoId, expedicaoUuid);
        List<com.tcc.pjb.backend.core.comunicacao.institucional.delivery.domain.InstitutionalDeliveryJob> jobs = selectJobs(processoId, expedicaoUuid);
        List<InstitutionalDelegationAssignment> delegations = selectDelegations(processoId, expedicaoUuid);
        List<InstitutionalDraftManifestation> drafts = selectDrafts(processoId, expedicaoUuid);
        Map<String, InstitutionalFlowAnalyticsBucket> falhas = summarize(jobs, job -> job.lastFailureReason() == null ? "SEM_FALHA" : job.lastFailureReason().name(), item -> 0d);
        Map<String, InstitutionalFlowAnalyticsBucket> redistribuicoes = summarize(
                inbox.stream().filter(item -> item.justificativas().stream().anyMatch(v -> v.startsWith("redistribuicao_interna="))).toList(),
                this::extractAtoCanonico,
                item -> 0d
        );
        Map<String, InstitutionalFlowAnalyticsBucket> minutas = summarize(drafts, draft -> draft.status().name(), draft -> 0d);
        Map<String, InstitutionalFlowAnalyticsBucket> delegacoesPorTipo = summarize(delegations, assignment -> assignment.tipoFluxo().name(), assignment -> 0d);
        double mediaCienciaCumprimento = averageHours(inbox, item -> item.cientificadaEm(), item -> item.cumpridaEm());
        double mediaCienciaPeticao = averageHours(inbox, item -> item.cientificadaEm(), item -> item.cumpridaEm());
        long totalDelegacoesAtivas = delegations.stream().filter(item -> item.status() == StatusDelegacaoInstitucional.ATIVA && item.ativaEm(Instant.now())).count();
        long totalSubstituicoesAtivas = delegations.stream().filter(item -> item.status() == StatusDelegacaoInstitucional.ATIVA && item.tipoFluxo().isSubstituicao() && item.ativaEm(Instant.now())).count();
        long totalMinutasPendentes = drafts.stream().filter(item -> item.status() == StatusMinutaInstitucional.EM_APROVACAO).count();
        List<String> insights = new ArrayList<>();
        if (!falhas.isEmpty()) {
            insights.add("Principais motivos de fallback/falha: " + String.join(", ", falhas.keySet().stream().limit(3).toList()));
        }
        if (!redistribuicoes.isEmpty()) {
            insights.add("Atos com maior redistribuição interna: " + String.join(", ", redistribuicoes.keySet().stream().limit(3).toList()));
        }
        if (totalMinutasPendentes > 0) {
            insights.add("Há " + totalMinutasPendentes + " minuta(s) aguardando aprovação institucional.");
        }
        if (totalSubstituicoesAtivas > 0) {
            insights.add("Há " + totalSubstituicoesAtivas + " substituição(ões) ativa(s) sustentando continuidade operacional.");
        }
        InstitutionalFlowAnalyticsDashboard dashboard = new InstitutionalFlowAnalyticsDashboard(
                falhas,
                redistribuicoes,
                minutas,
                delegacoesPorTipo,
                mediaCienciaCumprimento,
                mediaCienciaPeticao,
                totalDelegacoesAtivas,
                totalSubstituicoesAtivas,
                totalMinutasPendentes,
                List.copyOf(insights),
                Instant.now()
        );
        if (global) {
            globalDashboardCache.set(new CachedDashboard(dashboard, Instant.now().plus(GLOBAL_CACHE_TTL)));
        } else {
            ScopedDashboardKey key = new ScopedDashboardKey(processoId, normalizeExpedicaoUuid(expedicaoUuid));
            scopedDashboardCache.put(key, new CachedDashboard(dashboard, Instant.now().plus(SCOPED_CACHE_TTL)));
            trimScopedCache();
        }
        return dashboard;
    }


    private boolean isFresh(CachedDashboard cache) {
        return cache != null && cache.expiresAt() != null && cache.expiresAt().isAfter(Instant.now());
    }

    private String normalizeExpedicaoUuid(String expedicaoUuid) {
        if (expedicaoUuid == null || expedicaoUuid.isBlank()) {
            return null;
        }
        return expedicaoUuid.trim().toUpperCase(Locale.ROOT);
    }

    private void trimScopedCache() {
        Instant now = Instant.now();
        scopedDashboardCache.entrySet().removeIf(entry -> !isFresh(entry.getValue()) || entry.getValue().expiresAt().isBefore(now));
        if (scopedDashboardCache.size() <= MAX_SCOPED_CACHE) {
            return;
        }
        int removeCount = scopedDashboardCache.size() - MAX_SCOPED_CACHE;
        scopedDashboardCache.entrySet().stream()
                .sorted(Comparator.comparing(entry -> entry.getValue().expiresAt()))
                .limit(removeCount)
                .map(Map.Entry::getKey)
                .toList()
                .forEach(scopedDashboardCache::remove);
    }

    @Transactional(readOnly = true)
    public List<InstitutionalSemanticTimelineEntry> timelineSemantica(String expedicaoUuid) {
        return timelineRepository.findByExpedicaoUuid(expedicaoUuid).stream()
                .sorted(Comparator.comparing(InstitutionalTimelineEvent::occurredAt))
                .map(this::toSemantic)
                .toList();
    }

    private InstitutionalSemanticTimelineEntry toSemantic(InstitutionalTimelineEvent event) {
        String icon;
        String title;
        String phase;
        String desc = event.resumo();
        switch (event.eventType()) {
            case COMUNICACAO_DISPONIBILIZADA -> { icon = "📨"; title = "Remessa institucional criada"; phase = "REMESSA"; }
            case COMUNICACAO_RECEBIDA -> { icon = "📥"; title = "Recebimento pela caixa"; phase = "TRIAGEM"; }
            case COMUNICACAO_REDISTRIBUIDA -> { icon = "🔀"; title = "Redistribuição interna"; phase = "TRIAGEM"; }
            case COMUNICACAO_CIENTIFICADA, CERTIDAO_GERADA -> { icon = "👁️"; title = "Ciência institucional"; phase = "CIENCIA"; }
            case COMUNICACAO_CUMPRIDA -> { icon = "🖋️"; title = "Manifestação ou cumprimento"; phase = "CUMPRIMENTO"; }
            case GATE_CRIADO, GATE_TRANSICIONADO, GATE_LIBERADO -> { icon = "🛡️"; title = "Gate processual"; phase = "CONTROLE"; }
            case DELEGACAO_REGISTRADA -> { icon = "👤"; title = "Delegação interna"; phase = "TRIAGEM"; }
            case SUBSTITUICAO_REGISTRADA -> { icon = "🔁"; title = "Substituição operacional"; phase = "TRIAGEM"; }
            case MINUTA_CRIADA -> { icon = "📝"; title = "Minuta criada"; phase = "MINUTA"; }
            case MINUTA_ENCAMINHADA_APROVACAO -> { icon = "📤"; title = "Minuta em aprovação"; phase = "MINUTA"; }
            case MINUTA_APROVADA -> { icon = "✅"; title = "Minuta aprovada"; phase = "MINUTA"; }
            case MINUTA_REJEITADA -> { icon = "⛔"; title = "Minuta rejeitada"; phase = "MINUTA"; }
            case ENTREGA_ENFILEIRADA, ENTREGA_TENTADA, ENTREGA_ENCAMINHADA, ENTREGA_CONFIRMADA, ENTREGA_RETRY_AGENDADO, ENTREGA_FALHA_TERMINAL, ENTREGA_MOVIDA_DLQ -> {
                icon = "🚚"; title = "Entrega e integração"; phase = "ENTREGA";
            }
            default -> { icon = "•"; title = event.eventType().name(); phase = "GERAL"; }
        }
        return new InstitutionalSemanticTimelineEntry(event.eventId(), icon, title, desc, phase, event.occurredAt());
    }

    private <T> Map<String, InstitutionalFlowAnalyticsBucket> summarize(List<T> items,
                                                                        Function<T, String> classifier,
                                                                        Function<T, Double> hourExtractor) {
        LinkedHashMap<String, InstitutionalFlowAnalyticsBucket> out = new LinkedHashMap<>();
        long total = items.size();
        Map<String, List<T>> grouped = new LinkedHashMap<>();
        for (T item : items) {
            String key = classifier.apply(item);
            key = key == null || key.isBlank() ? "DESCONHECIDO" : key.trim().toUpperCase(Locale.ROOT);
            grouped.computeIfAbsent(key, ignored -> new ArrayList<>()).add(item);
        }
        grouped.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue().size(), a.getValue().size()))
                .forEach(entry -> {
                    double avg = entry.getValue().stream().map(hourExtractor).filter(Objects::nonNull).mapToDouble(Double::doubleValue).average().orElse(0d);
                    double pct = total == 0 ? 0d : (entry.getValue().size() * 100d) / total;
                    out.put(entry.getKey(), new InstitutionalFlowAnalyticsBucket("BUCKET", entry.getKey(), entry.getValue().size(), pct, avg));
                });
        return Collections.unmodifiableMap(out);
    }

    private double averageHours(List<InstitutionalInboxItem> items,
                                Function<InstitutionalInboxItem, Instant> start,
                                Function<InstitutionalInboxItem, Instant> end) {
        return items.stream()
                .filter(item -> start.apply(item) != null && end.apply(item) != null)
                .mapToDouble(item -> Duration.between(start.apply(item), end.apply(item)).toMinutes() / 60d)
                .average()
                .orElse(0d);
    }

    private String extractAtoCanonico(InstitutionalInboxItem item) {
        return item.justificativas().stream()
                .filter(v -> v.startsWith("atoCanonico="))
                .map(v -> v.substring("atoCanonico=".length()))
                .findFirst()
                .orElse("SEM_ATO_CANONICO");
    }

    private List<InstitutionalInboxItem> selectInbox(Long processoId, String expedicaoUuid) {
        if (expedicaoUuid != null && !expedicaoUuid.isBlank()) {
            return inboxRepository.findByExpedicaoUuid(expedicaoUuid).stream().toList();
        }
        return processoId == null ? inboxRepository.findAll() : inboxRepository.findByProcessoId(processoId);
    }

    private List<com.tcc.pjb.backend.core.comunicacao.institucional.delivery.domain.InstitutionalDeliveryJob> selectJobs(Long processoId, String expedicaoUuid) {
        if (expedicaoUuid != null && !expedicaoUuid.isBlank()) {
            return deliveryJobRepository.findByExpedicaoUuid(expedicaoUuid);
        }
        return processoId == null ? deliveryJobRepository.findAll() : deliveryJobRepository.findByProcessoId(processoId);
    }

    private List<InstitutionalDelegationAssignment> selectDelegations(Long processoId, String expedicaoUuid) {
        if (expedicaoUuid != null && !expedicaoUuid.isBlank()) {
            return delegationRepository.findByExpedicaoUuid(expedicaoUuid);
        }
        return processoId == null ? delegationRepository.findAll() : delegationRepository.findByProcessoId(processoId);
    }

    private List<InstitutionalDraftManifestation> selectDrafts(Long processoId, String expedicaoUuid) {
        if (expedicaoUuid != null && !expedicaoUuid.isBlank()) {
            return draftRepository.findByExpedicaoUuid(expedicaoUuid);
        }
        return processoId == null ? draftRepository.findAll() : draftRepository.findByProcessoId(processoId);
    }

    private record CachedDashboard(InstitutionalFlowAnalyticsDashboard dashboard, Instant expiresAt) { }

    private record ScopedDashboardKey(Long processoId, String expedicaoUuid) { }
}
