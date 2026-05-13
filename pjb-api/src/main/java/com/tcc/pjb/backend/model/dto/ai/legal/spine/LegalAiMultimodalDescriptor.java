package com.tcc.pjb.backend.model.dto.ai.legal.spine;

import java.util.List;
import java.util.Map;

public record LegalAiMultimodalDescriptor(
        List<String> enabledModalities,
        boolean evidenceIngestionEnabled,
        boolean provenanceRequired,
        Map<String, Object> multimodalPolicy
) {
    public Map<String, Object> asMap() {
        return Map.of(
                "enabledModalities", enabledModalities == null ? List.of() : List.copyOf(enabledModalities),
                "evidenceIngestionEnabled", evidenceIngestionEnabled,
                "provenanceRequired", provenanceRequired,
                "multimodalPolicy", multimodalPolicy == null ? Map.of() : Map.copyOf(multimodalPolicy)
        );
    }
}
