package com.tcc.pjb.backend.model.dto.processual.observability.continuity;

import java.time.Instant;
import java.util.List;
import com.tcc.pjb.backend.core.processo.lifecycle.ProcessoLifecycleAction;
import com.tcc.pjb.backend.model.entity.casefile.CaseContinuityTrack;

public record CaseContinuityDecisionGateResponse(
        Instant generatedAt,
        Long caseFileId,
        Long requestedProcessoId,
        ProcessoLifecycleAction action,
        String canonicalActType,
        CaseContinuityTrack dominantTrack,
        CaseContinuityTrack expectedTrack,
        CaseContinuityReadinessLevel readinessLevel,
        boolean allowed,
        boolean sensitive,
        boolean lifecycleAllowsAction,
        boolean securityVisible,
        boolean integrationHealthy,
        boolean recursalMatrixReady,
        boolean financialAiReady,
        List<String> warnings,
        List<String> blockers,
        List<String> recommendedActions
) {
    public CaseContinuityDecisionGateResponse {
        generatedAt = generatedAt == null ? Instant.now() : generatedAt;
        readinessLevel = readinessLevel == null ? CaseContinuityReadinessLevel.ALERTA : readinessLevel;
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
        blockers = blockers == null ? List.of() : List.copyOf(blockers);
        recommendedActions = recommendedActions == null ? List.of() : List.copyOf(recommendedActions);
    }
}
