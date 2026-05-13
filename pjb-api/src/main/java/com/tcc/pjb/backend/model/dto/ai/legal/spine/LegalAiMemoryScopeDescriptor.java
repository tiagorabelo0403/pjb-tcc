package com.tcc.pjb.backend.model.dto.ai.legal.spine;

import java.util.List;
import java.util.Map;

public record LegalAiMemoryScopeDescriptor(
        List<String> enabledScopes,
        boolean strictIsolation,
        boolean crossCaseReuseBlocked,
        Map<String, Object> memoryPolicy
) {
    public Map<String, Object> asMap() {
        return Map.of(
                "enabledScopes", enabledScopes == null ? List.of() : List.copyOf(enabledScopes),
                "strictIsolation", strictIsolation,
                "crossCaseReuseBlocked", crossCaseReuseBlocked,
                "memoryPolicy", memoryPolicy == null ? Map.of() : Map.copyOf(memoryPolicy)
        );
    }
}
