package com.tcc.pjb.backend.model.dto.ai.legal;

import java.util.List;
import java.util.Map;

public record LegalValidationResponse(
        String profileCode,
        String version,
        String capability,
        String status,
        boolean citationFirst,
        String approvalMode,
        List<String> symbolicEngines,
        List<String> evalSuites,
        List<String> missingEvidence,
        List<String> contradictions,
        String recommendedSchema,
        Map<String, Object> trace
) {
}
