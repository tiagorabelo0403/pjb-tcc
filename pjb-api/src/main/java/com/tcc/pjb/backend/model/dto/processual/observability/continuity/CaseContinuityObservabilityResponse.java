package com.tcc.pjb.backend.model.dto.processual.observability.continuity;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import com.tcc.pjb.backend.model.entity.casefile.CaseContinuityTrack;

public record CaseContinuityObservabilityResponse(
        Instant generatedAt,
        Long caseFileId,
        Long rootProcessoId,
        Long requestedProcessoId,
        String anchorProceedingKey,
        CaseContinuityTrack dominantTrack,
        long proceedingCount,
        long edgeCount,
        long eventCount,
        long recursalBranches,
        long executoryBranches,
        long archivedBranches,
        long reactivatedBranches,
        long shadowProceedings,
        long staleProceedings,
        Instant latestEventAt,
        Map<String, Long> proceedingsByTrack,
        Map<String, Long> proceedingsByRole,
        Map<String, Long> proceedingsByStatus,
        List<String> recentEventTypes,
        List<String> warnings,
        boolean unifiedRoot,
        boolean attentionRequired
) {
    public CaseContinuityObservabilityResponse {
        generatedAt = generatedAt == null ? Instant.now() : generatedAt;
        proceedingsByTrack = proceedingsByTrack == null ? Map.of() : Map.copyOf(proceedingsByTrack);
        proceedingsByRole = proceedingsByRole == null ? Map.of() : Map.copyOf(proceedingsByRole);
        proceedingsByStatus = proceedingsByStatus == null ? Map.of() : Map.copyOf(proceedingsByStatus);
        recentEventTypes = recentEventTypes == null ? List.of() : List.copyOf(recentEventTypes);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }
}
