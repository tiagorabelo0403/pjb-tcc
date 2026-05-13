package com.tcc.pjb.backend.model.dto.ai.legal.schema;

import java.util.List;
import java.util.Map;

public record LegalAiSchemaDefinition(
        String schemaId,
        String label,
        String stage,
        List<String> supportedVersions,
        List<String> audienceProfiles,
        List<String> requiredKeys,
        boolean citationFirst,
        boolean symbolicValidationRequired,
        List<LegalAiSchemaField> fields,
        Map<String, Object> example
) {
    public Map<String, Object> asMap() {
        return Map.of(
                "schemaId", schemaId,
                "label", label,
                "stage", stage,
                "supportedVersions", supportedVersions == null ? List.of() : List.copyOf(supportedVersions),
                "audienceProfiles", audienceProfiles == null ? List.of() : List.copyOf(audienceProfiles),
                "requiredKeys", requiredKeys == null ? List.of() : List.copyOf(requiredKeys),
                "citationFirst", citationFirst,
                "symbolicValidationRequired", symbolicValidationRequired,
                "fields", fields == null ? List.of() : fields.stream().map(LegalAiSchemaField::asMap).toList(),
                "example", example == null ? Map.of() : Map.copyOf(example)
        );
    }
}
