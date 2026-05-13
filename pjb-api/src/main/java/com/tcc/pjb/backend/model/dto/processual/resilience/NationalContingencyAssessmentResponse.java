package com.tcc.pjb.backend.model.dto.processual.resilience;

import java.util.List;
import java.util.Map;

public record NationalContingencyAssessmentResponse(
        Long processoId,
        String numeroProcesso,
        String judicialSystem,
        String contingencyMode,
        boolean operatingNormally,
        boolean manualFallbackRequired,
        boolean queueRetryRecommended,
        boolean dryRunOnly,
        boolean officialJusticeEscalationRecommended,
        List<String> blockers,
        List<String> warnings,
        List<String> actions,
        Map<String, Object> metadata
) {
    public NationalContingencyAssessmentResponse {
        blockers = blockers == null ? List.of() : List.copyOf(blockers);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
        actions = actions == null ? List.of() : List.copyOf(actions);
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
