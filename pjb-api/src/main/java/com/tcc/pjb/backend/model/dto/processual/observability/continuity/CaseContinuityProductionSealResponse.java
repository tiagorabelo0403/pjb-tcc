package com.tcc.pjb.backend.model.dto.processual.observability.continuity;

import java.time.Instant;
import java.util.List;
import com.tcc.pjb.backend.model.entity.casefile.CaseContinuityTrack;

public record CaseContinuityProductionSealResponse(
        Instant generatedAt,
        Long caseFileId,
        Long requestedProcessoId,
        CaseContinuityTrack dominantTrack,
        CaseContinuityTrack expectedTrack,
        CaseContinuityReadinessLevel readinessLevel,
        CaseContinuityProductionSealLevel sealLevel,
        boolean healthy,
        long auditedActionCount,
        long allowedSensitiveActions,
        long blockedSensitiveActions,
        boolean lifecycleConnected,
        boolean securityConnected,
        boolean recursalMatrixReady,
        boolean financialAiReady,
        boolean structuredContinuation,
        boolean autoRepairEligible,
        List<String> auditedActions,
        List<String> blockedActions,
        List<String> releaseCriteria,
        List<String> warnings,
        List<String> blockers,
        List<String> recommendedActions
) {
    public CaseContinuityProductionSealResponse {
        generatedAt = generatedAt == null ? Instant.now() : generatedAt;
        readinessLevel = readinessLevel == null ? CaseContinuityReadinessLevel.ALERTA : readinessLevel;
        sealLevel = sealLevel == null ? CaseContinuityProductionSealLevel.CONDICIONAL : sealLevel;
        auditedActions = auditedActions == null ? List.of() : List.copyOf(auditedActions);
        blockedActions = blockedActions == null ? List.of() : List.copyOf(blockedActions);
        releaseCriteria = releaseCriteria == null ? List.of() : List.copyOf(releaseCriteria);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
        blockers = blockers == null ? List.of() : List.copyOf(blockers);
        recommendedActions = recommendedActions == null ? List.of() : List.copyOf(recommendedActions);
    }
}
