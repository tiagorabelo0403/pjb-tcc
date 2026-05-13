package com.tcc.pjb.backend.model.dto.ai.legal.conversation;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record LegalAiConversationSanitizationSnapshot(
        String status,
        boolean promptInjectionDetected,
        List<String> alerts,
        Map<String, Object> diagnostics
) {
    public Map<String, Object> asMap() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("status", status);
        out.put("promptInjectionDetected", promptInjectionDetected);
        out.put("alerts", alerts == null ? List.of() : List.copyOf(alerts));
        out.put("diagnostics", diagnostics == null ? Map.of() : Map.copyOf(diagnostics));
        return Collections.unmodifiableMap(out);
    }
}
