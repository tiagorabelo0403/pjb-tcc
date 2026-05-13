package com.tcc.pjb.backend.model.dto.ai.legal.conversation;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record LegalAiConversationTraceSnapshot(
        String traceId,
        String turnId,
        String lane,
        String status,
        List<String> auditFields,
        Map<String, Object> diagnostics,
        List<Map<String, Object>> executionTrail
) {
    public Map<String, Object> asMap() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("traceId", traceId);
        out.put("turnId", turnId);
        out.put("lane", lane);
        out.put("status", status);
        out.put("auditFields", auditFields == null ? List.of() : List.copyOf(auditFields));
        out.put("diagnostics", diagnostics == null ? Map.of() : Map.copyOf(diagnostics));
        out.put("executionTrail", executionTrail == null ? List.of() : executionTrail.stream().map(Map::copyOf).toList());
        return Collections.unmodifiableMap(out);
    }
}
