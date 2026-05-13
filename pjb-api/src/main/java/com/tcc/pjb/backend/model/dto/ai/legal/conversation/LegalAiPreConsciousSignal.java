package com.tcc.pjb.backend.model.dto.ai.legal.conversation;

import com.tcc.pjb.backend.ai.juridica.conversation.ImmutableViewSupport;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record LegalAiPreConsciousSignal(
        String type,
        String severity,
        String code,
        String message,
        String source,
        Map<String, Object> metadata
) {
    public LegalAiPreConsciousSignal {
        type = safe(type);
        severity = safe(severity);
        code = safe(code);
        message = safe(message);
        source = safe(source);
        metadata = ImmutableViewSupport.map(metadata == null ? Map.of() : metadata);
    }

    public static LegalAiPreConsciousSignal critical(String code, String message, String source) {
        return new LegalAiPreConsciousSignal("RISK", "CRITICAL", code, message, source, Map.of());
    }

    public static LegalAiPreConsciousSignal high(String code, String message, String source) {
        return new LegalAiPreConsciousSignal("RISK", "HIGH", code, message, source, Map.of());
    }

    public static LegalAiPreConsciousSignal medium(String code, String message, String source) {
        return new LegalAiPreConsciousSignal("GOVERNANCE", "MEDIUM", code, message, source, Map.of());
    }

    public static LegalAiPreConsciousSignal learning(String code, String message, String source) {
        return new LegalAiPreConsciousSignal("LEARNING", "MEDIUM", code, message, source, Map.of());
    }

    public Map<String, Object> asMap() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("type", type);
        out.put("severity", severity);
        out.put("code", code);
        out.put("message", message);
        out.put("source", source);
        out.put("metadata", metadata);
        return Collections.unmodifiableMap(out);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
