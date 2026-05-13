package com.tcc.pjb.backend.model.dto.ai.legal.eval;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record LegalEvalSuite(
        String suiteId,
        String label,
        String scope,
        String version,
        List<LegalEvalCase> cases
) {
    public Map<String, Object> asMap() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("suiteId", suiteId);
        out.put("label", label);
        out.put("scope", scope);
        out.put("version", version);
        out.put("cases", cases == null ? List.of() : cases.stream().map(LegalEvalCase::asMap).toList());
        return Collections.unmodifiableMap(out);
    }
}
