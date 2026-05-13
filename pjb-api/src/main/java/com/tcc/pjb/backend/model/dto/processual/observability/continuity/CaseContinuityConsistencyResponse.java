package com.tcc.pjb.backend.model.dto.processual.observability.continuity;

import java.time.Instant;
import java.util.List;
import com.tcc.pjb.backend.model.entity.casefile.CaseContinuityTrack;

public record CaseContinuityConsistencyResponse(
        Instant generatedAt,
        Long caseFileId,
        Long requestedProcessoId,
        CaseContinuityTrack dominantTrack,
        boolean healthy,
        long proceedingCount,
        long rootProceedingCount,
        long orphanParentCount,
        long incompatibleRoleTrackCount,
        long incompatibleStateCount,
        long recursalBranchesWithoutEdge,
        long executoryBranchesWithoutParent,
        long staleProceedings,
        List<String> warnings,
        List<String> inconsistencies,
        List<String> recommendedActions
) {
    public CaseContinuityConsistencyResponse {
        generatedAt = generatedAt == null ? Instant.now() : generatedAt;
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
        inconsistencies = inconsistencies == null ? List.of() : List.copyOf(inconsistencies);
        recommendedActions = recommendedActions == null ? List.of() : List.copyOf(recommendedActions);
    }
}
