package com.tcc.pjb.backend.integration.judicial;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record JudicialConnectorPolicyReport(
        Instant generatedAt,
        String environmentName,
        int activePolicyCount,
        List<Map<String, Object>> policies,
        List<String> blockers,
        List<String> warnings,
        Map<String, Object> metadata
) {
    public Map<String, Object> toMap() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("generatedAt", generatedAt != null ? generatedAt.toString() : null);
        out.put("environmentName", environmentName);
        out.put("activePolicyCount", activePolicyCount);
        out.put("policies", policies == null ? List.of() : policies);
        out.put("blockers", blockers == null ? List.of() : blockers);
        out.put("warnings", warnings == null ? List.of() : warnings);
        out.put("metadata", metadata == null ? Map.of() : metadata);
        out.entrySet().removeIf(entry -> entry.getValue() == null);
        return Map.copyOf(out);
    }
}
