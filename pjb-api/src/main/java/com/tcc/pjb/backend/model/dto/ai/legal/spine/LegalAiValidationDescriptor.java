package com.tcc.pjb.backend.model.dto.ai.legal.spine;

import java.util.List;
import java.util.Map;

public record LegalAiValidationDescriptor(
        List<String> symbolicEngines,
        boolean evalsEnabled,
        boolean citationGroundingRequired,
        Map<String, Object> validationPolicy
) {
    public Map<String, Object> asMap() {
        return Map.of(
                "symbolicEngines", symbolicEngines == null ? List.of() : List.copyOf(symbolicEngines),
                "evalsEnabled", evalsEnabled,
                "citationGroundingRequired", citationGroundingRequired,
                "validationPolicy", validationPolicy == null ? Map.of() : Map.copyOf(validationPolicy)
        );
    }
}
