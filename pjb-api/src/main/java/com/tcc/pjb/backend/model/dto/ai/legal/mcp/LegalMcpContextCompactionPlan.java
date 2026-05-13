package com.tcc.pjb.backend.model.dto.ai.legal.mcp;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record LegalMcpContextCompactionPlan(
        String status,
        String policy,
        int retainedHistoryBudget,
        String externalizationMode,
        List<String> reasons
) {
    public Map<String, Object> asMap() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("status", status);
        out.put("policy", policy);
        out.put("retainedHistoryBudget", retainedHistoryBudget);
        out.put("externalizationMode", externalizationMode);
        out.put("reasons", reasons == null ? List.of() : List.copyOf(reasons));
        return Collections.unmodifiableMap(out);
    }
}
