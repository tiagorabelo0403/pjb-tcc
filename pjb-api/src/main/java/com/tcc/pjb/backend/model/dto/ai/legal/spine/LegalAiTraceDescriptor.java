package com.tcc.pjb.backend.model.dto.ai.legal.spine;

import java.util.List;
import java.util.Map;

public record LegalAiTraceDescriptor(
        boolean enabled,
        String lane,
        List<String> requiredAuditFields,
        Map<String, Object> tracePolicy
) {
    public Map<String, Object> asMap() {
        return Map.of(
                "enabled", enabled,
                "lane", lane,
                "requiredAuditFields", requiredAuditFields == null ? List.of() : List.copyOf(requiredAuditFields),
                "tracePolicy", tracePolicy == null ? Map.of() : Map.copyOf(tracePolicy)
        );
    }
}
