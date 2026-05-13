package com.tcc.pjb.backend.model.dto.processual.observability.continuity;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import com.tcc.pjb.backend.model.entity.casefile.CaseContinuityTrack;

public record CaseContinuityIntegrationResponse(
        Instant generatedAt,
        Long caseFileId,
        Long requestedProcessoId,
        CaseContinuityTrack dominantTrack,
        CaseContinuityTrack expectedTrack,
        CaseContinuityReadinessLevel readinessLevel,
        boolean healthy,
        boolean lifecycleConnected,
        boolean securityConnected,
        boolean recursalMatrixReady,
        boolean financialAiReady,
        boolean structuredContinuation,
        String financialAiVersion,
        List<String> financialAiCapabilities,
        List<String> candidateAppealCodes,
        List<String> unresolvedAppealTypes,
        Map<String, String> supportedAppealLabels,
        List<String> warnings,
        List<String> blockers,
        List<String> recommendedActions
) {
    public CaseContinuityIntegrationResponse {
        generatedAt = generatedAt == null ? Instant.now() : generatedAt;
        readinessLevel = readinessLevel == null ? CaseContinuityReadinessLevel.ALERTA : readinessLevel;
        financialAiCapabilities = financialAiCapabilities == null ? List.of() : List.copyOf(financialAiCapabilities);
        candidateAppealCodes = candidateAppealCodes == null ? List.of() : List.copyOf(candidateAppealCodes);
        unresolvedAppealTypes = unresolvedAppealTypes == null ? List.of() : List.copyOf(unresolvedAppealTypes);
        supportedAppealLabels = supportedAppealLabels == null ? Map.of() : Map.copyOf(supportedAppealLabels);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
        blockers = blockers == null ? List.of() : List.copyOf(blockers);
        recommendedActions = recommendedActions == null ? List.of() : List.copyOf(recommendedActions);
    }
}
