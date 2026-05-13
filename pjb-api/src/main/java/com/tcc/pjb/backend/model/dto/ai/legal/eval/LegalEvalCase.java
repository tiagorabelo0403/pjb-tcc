package com.tcc.pjb.backend.model.dto.ai.legal.eval;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record LegalEvalCase(
        String caseId,
        String label,
        String description,
        String expectedSelectionMode,
        List<String> requiredPinnedServers,
        List<String> requiredSafeguards,
        Integer minEvidenceBudget,
        Integer maxServerBudget,
        String expectedTrustMode
) {
    public Map<String, Object> asMap() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("caseId", caseId);
        out.put("label", label);
        out.put("description", description);
        out.put("expectedSelectionMode", expectedSelectionMode);
        out.put("requiredPinnedServers", requiredPinnedServers == null ? List.of() : List.copyOf(requiredPinnedServers));
        out.put("requiredSafeguards", requiredSafeguards == null ? List.of() : List.copyOf(requiredSafeguards));
        out.put("minEvidenceBudget", minEvidenceBudget);
        out.put("maxServerBudget", maxServerBudget);
        out.put("expectedTrustMode", expectedTrustMode);
        return Collections.unmodifiableMap(out);
    }
}
