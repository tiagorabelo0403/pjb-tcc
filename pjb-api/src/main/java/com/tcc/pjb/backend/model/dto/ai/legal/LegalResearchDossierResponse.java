package com.tcc.pjb.backend.model.dto.ai.legal;

import java.util.List;
import java.util.Map;

public record LegalResearchDossierResponse(
        String profileCode,
        String version,
        String capability,
        List<String> retrievalStages,
        List<String> authorityLanes,
        List<String> graphTraversals,
        List<String> toolIds,
        List<String> recommendedSchemas,
        List<Map<String, Object>> findings,
        Map<String, Object> trace
) {
}
