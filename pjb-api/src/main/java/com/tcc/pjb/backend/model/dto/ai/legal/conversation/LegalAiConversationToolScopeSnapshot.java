package com.tcc.pjb.backend.model.dto.ai.legal.conversation;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record LegalAiConversationToolScopeSnapshot(
        String status,
        List<String> allowedToolIds,
        List<String> blockedToolIds,
        List<String> stepUpToolIds,
        List<String> reasons,
        Map<String, Object> diagnostics
) {
    public Map<String, Object> asMap() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("status", status);
        out.put("allowedToolIds", safeList(allowedToolIds));
        out.put("blockedToolIds", safeList(blockedToolIds));
        out.put("stepUpToolIds", safeList(stepUpToolIds));
        out.put("reasons", safeList(reasons));
        out.put("diagnostics", safeMap(diagnostics));
        return Collections.unmodifiableMap(out);
    }

    private static List<String> safeList(List<String> source) {
        if (source == null || source.isEmpty()) {
            return List.of();
        }
        return source.stream()
                .filter(Objects::nonNull)
                .filter(value -> !value.isBlank())
                .toList();
    }

    private static Map<String, Object> safeMap(Map<String, Object> source) {
        if (source == null || source.isEmpty()) {
            return Map.of();
        }
        LinkedHashMap<String, Object> sanitized = new LinkedHashMap<>();
        source.forEach((key, value) -> {
            if (key != null && value != null) {
                sanitized.put(key, value);
            }
        });
        return Map.copyOf(sanitized);
    }
}
