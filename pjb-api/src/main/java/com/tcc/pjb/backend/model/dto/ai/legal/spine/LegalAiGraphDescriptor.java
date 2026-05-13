package com.tcc.pjb.backend.model.dto.ai.legal.spine;

import java.util.List;
import java.util.Map;

public record LegalAiGraphDescriptor(
        boolean enabled,
        List<String> traversalModes,
        List<String> nodeFamilies,
        List<String> edgeFamilies,
        Map<String, Object> graphPolicy
) {
    public Map<String, Object> asMap() {
        return Map.of(
                "enabled", enabled,
                "traversalModes", traversalModes == null ? List.of() : List.copyOf(traversalModes),
                "nodeFamilies", nodeFamilies == null ? List.of() : List.copyOf(nodeFamilies),
                "edgeFamilies", edgeFamilies == null ? List.of() : List.copyOf(edgeFamilies),
                "graphPolicy", graphPolicy == null ? Map.of() : Map.copyOf(graphPolicy)
        );
    }
}
