package com.tcc.pjb.backend.model.dto.processual.observability.continuity;

import java.time.Instant;
import java.util.List;
import com.tcc.pjb.backend.model.entity.casefile.CaseContinuityTrack;

public record CaseContinuityRemediationResponse(
        Instant generatedAt,
        Long caseFileId,
        Long requestedProcessoId,
        CaseContinuityTrack dominantTrack,
        CaseContinuityTrack expectedTrack,
        CaseContinuityReadinessLevel readinessLevel,
        boolean healthy,
        boolean autoRepairEligible,
        long totalIssues,
        long automatedActionCount,
        long manualActionCount,
        List<String> automatedRepairActions,
        List<String> manualRepairActions,
        List<String> warnings,
        List<String> blockers,
        List<String> recommendedActions
) {
    public CaseContinuityRemediationResponse {
        generatedAt = generatedAt == null ? Instant.now() : generatedAt;
        readinessLevel = readinessLevel == null ? CaseContinuityReadinessLevel.ALERTA : readinessLevel;
        automatedRepairActions = automatedRepairActions == null ? List.of() : List.copyOf(automatedRepairActions);
        manualRepairActions = manualRepairActions == null ? List.of() : List.copyOf(manualRepairActions);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
        blockers = blockers == null ? List.of() : List.copyOf(blockers);
        recommendedActions = recommendedActions == null ? List.of() : List.copyOf(recommendedActions);
    }
}
