package com.tcc.pjb.backend.model.dto.ai.legal.schema;

import java.util.List;
import java.util.Map;

public record LegalAiSchemaField(
        String key,
        String type,
        boolean required,
        String description,
        List<LegalAiSchemaField> children
) {
    public Map<String, Object> asMap() {
        return Map.of(
                "key", key,
                "type", type,
                "required", required,
                "description", description == null ? "" : description,
                "children", children == null ? List.of() : children.stream().map(LegalAiSchemaField::asMap).toList()
        );
    }
}
