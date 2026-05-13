package com.tcc.pjb.backend.model.dto.ai.legal.spine;

import java.util.List;
import java.util.Map;

public record LegalAiStructuredOutputDescriptor(
        String schemaId,
        String stage,
        List<String> requiredKeys,
        boolean citationFirst,
        boolean symbolicValidationRequired,
        Map<String, Object> schemaHints
) {
    public Map<String, Object> asMap() {
        return Map.of(
                "schemaId", schemaId,
                "stage", stage,
                "requiredKeys", requiredKeys == null ? List.of() : List.copyOf(requiredKeys),
                "citationFirst", citationFirst,
                "symbolicValidationRequired", symbolicValidationRequired,
                "schemaHints", schemaHints == null ? Map.of() : Map.copyOf(schemaHints)
        );
    }
}
