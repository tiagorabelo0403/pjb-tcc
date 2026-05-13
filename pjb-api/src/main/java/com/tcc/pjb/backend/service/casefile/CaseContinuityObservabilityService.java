package com.tcc.pjb.backend.service.casefile;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.kernel.casefile.CaseFileEventStore;
import com.tcc.pjb.backend.model.dto.processual.observability.continuity.CaseContinuityObservabilityResponse;
import com.tcc.pjb.backend.model.entity.kernel.CaseFileEventEnvelope;

@Service
public class CaseContinuityObservabilityService {

    private static final Duration STALE_THRESHOLD = Duration.ofHours(48);

    private final CaseContinuityOrchestratorService orchestratorService;
    private final CaseFileEventStore eventStore;
    private final AuditLedgerService auditLedgerService;
    private final CaseContinuityObservabilityMetrics metrics;

    public CaseContinuityObservabilityService(CaseContinuityOrchestratorService orchestratorService,
                                              CaseFileEventStore eventStore,
                                              AuditLedgerService auditLedgerService,
                                              CaseContinuityObservabilityMetrics metrics) {
        this.orchestratorService = Objects.requireNonNull(orchestratorService);
        this.eventStore = Objects.requireNonNull(eventStore);
        this.auditLedgerService = Objects.requireNonNull(auditLedgerService);
        this.metrics = Objects.requireNonNull(metrics);
    }

    @Transactional(readOnly = true)
    public CaseContinuityObservabilityResponse snapshot(Long processoId) {
        CaseContinuitySnapshot snapshot = orchestratorService.inspect(processoId);
        List<CaseFileEventEnvelope> events = eventStore.stream(snapshot.caseFileId());
        Instant reference = Instant.now();
        Map<String, Long> proceedingsByTrack = aggregate(snapshot.proceedings(), node -> node.continuityTrack() == null ? "NAO_CLASSIFICADO" : node.continuityTrack().name());
        Map<String, Long> proceedingsByRole = aggregate(snapshot.proceedings(), node -> node.role() == null ? "NAO_CLASSIFICADO" : node.role().name());
        Map<String, Long> proceedingsByStatus = aggregate(snapshot.proceedings(), node -> node.status() == null ? "NAO_CLASSIFICADO" : node.status().name());
        long recursalBranches = snapshot.proceedings().stream().filter(CaseContinuityProceedingNode::isRecursalBranch).count();
        long executoryBranches = snapshot.proceedings().stream().filter(CaseContinuityProceedingNode::isExecutoryBranch).count();
        long archivedBranches = snapshot.proceedings().stream().filter(CaseContinuityProceedingNode::isArchivedState).count();
        long reactivatedBranches = snapshot.proceedings().stream().filter(node -> node.continuityTrack() != null && node.continuityTrack().isReactivatedState()).count();
        long shadowProceedings = snapshot.proceedings().stream().filter(CaseContinuityProceedingNode::shadow).count();
        long staleProceedings = snapshot.proceedings().stream().filter(node -> node.isStale(reference, STALE_THRESHOLD)).count();
        Instant latestEventAt = events.stream().map(CaseFileEventEnvelope::getCreatedAt).filter(Objects::nonNull).max(Comparator.naturalOrder()).orElse(null);
        List<String> recentEventTypes = events.stream()
                .sorted(Comparator.comparing(CaseFileEventEnvelope::getSeq, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(8)
                .map(CaseFileEventEnvelope::getEventType)
                .toList();
        CaseContinuityObservabilityResponse response = new CaseContinuityObservabilityResponse(
                reference,
                snapshot.caseFileId(),
                snapshot.rootProcessoId(),
                snapshot.requestedProcessoId(),
                snapshot.anchorProceedingKey(),
                snapshot.dominantTrack(),
                snapshot.proceedingCount(),
                snapshot.edges().size(),
                events.size(),
                recursalBranches,
                executoryBranches,
                archivedBranches,
                reactivatedBranches,
                shadowProceedings,
                staleProceedings,
                latestEventAt,
                proceedingsByTrack,
                proceedingsByRole,
                proceedingsByStatus,
                recentEventTypes,
                snapshot.warnings(),
                snapshot.isUnifiedRoot(),
                snapshot.requiresAttention() || staleProceedings > 0
        );
        metrics.recordInspection(response);
        metrics.touchObservation(response.generatedAt());
        auditLedgerService.appendSafely("CASE_CONTINUITY_OBSERVABILITY_INSPECT", "CASE_FILE", String.valueOf(response.caseFileId()),
                String.join("|",
                        String.valueOf(response.caseFileId()),
                        String.valueOf(response.requestedProcessoId()),
                        response.dominantTrack() == null ? "-" : response.dominantTrack().name(),
                        String.valueOf(response.eventCount()),
                        String.valueOf(response.staleProceedings())));
        return response;
    }

    private static Map<String, Long> aggregate(List<CaseContinuityProceedingNode> proceedings, java.util.function.Function<CaseContinuityProceedingNode, String> classifier) {
        LinkedHashMap<String, Long> out = new LinkedHashMap<>();
        proceedings.stream()
                .map(classifier)
                .filter(Objects::nonNull)
                .sorted(Comparator.naturalOrder())
                .forEach(key -> out.merge(key, 1L, Long::sum));
        return out.isEmpty() ? Map.of() : Collections.unmodifiableMap(out);
    }
}
