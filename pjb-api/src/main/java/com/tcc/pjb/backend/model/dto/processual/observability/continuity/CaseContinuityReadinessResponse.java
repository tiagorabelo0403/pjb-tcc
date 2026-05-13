package com.tcc.pjb.backend.model.dto.processual.observability.continuity;

import java.time.Instant;
import java.util.List;
import com.tcc.pjb.backend.model.entity.casefile.CaseContinuityTrack;

public record CaseContinuityReadinessResponse(
        Instant generatedAt,
        Long caseFileId,
        Long requestedProcessoId,
        CaseContinuityTrack dominantTrack,
        CaseContinuityTrack expectedTrack,
        CaseContinuityReadinessLevel readinessLevel,
        boolean healthy,
        long totalAllowedActions,
        long totalBlockedActions,
        long totalSensitiveAllowedActions,
        long totalSensitiveBlockedActions,
        List<String> allowedActions,
        List<String> blockedActions,
        List<String> sensitiveAllowedActions,
        List<String> sensitiveBlockedActions,
        List<String> warnings,
        List<String> blockers,
        List<String> recommendedActions
) {
    public CaseContinuityReadinessResponse {
        generatedAt = generatedAt == null ? Instant.now() : generatedAt;
        allowedActions = allowedActions == null ? List.of() : List.copyOf(allowedActions);
        blockedActions = blockedActions == null ? List.of() : List.copyOf(blockedActions);
        sensitiveAllowedActions = sensitiveAllowedActions == null ? List.of() : List.copyOf(sensitiveAllowedActions);
        sensitiveBlockedActions = sensitiveBlockedActions == null ? List.of() : List.copyOf(sensitiveBlockedActions);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
        blockers = blockers == null ? List.of() : List.copyOf(blockers);
        recommendedActions = recommendedActions == null ? List.of() : List.copyOf(recommendedActions);
        readinessLevel = readinessLevel == null ? CaseContinuityReadinessLevel.ALERTA : readinessLevel;
    }
}
