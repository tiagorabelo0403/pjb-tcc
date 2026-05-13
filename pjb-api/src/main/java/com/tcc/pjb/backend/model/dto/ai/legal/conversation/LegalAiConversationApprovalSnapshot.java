package com.tcc.pjb.backend.model.dto.ai.legal.conversation;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record LegalAiConversationApprovalSnapshot(
        String status,
        boolean approvalRequired,
        boolean stepUpRequired,
        List<String> reasons,
        List<String> checkpoints,
        Map<String, Object> diagnostics
) {
    public LegalAiConversationApprovalSnapshot {
        reasons = safeList(reasons);
        checkpoints = safeList(checkpoints);
        diagnostics = safeMap(diagnostics);
    }

    public Map<String, Object> asMap() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("status", status);
        out.put("approvalRequired", approvalRequired);
        out.put("stepUpRequired", stepUpRequired);
        out.put("reasons", reasons);
        out.put("checkpoints", checkpoints);
        out.put("diagnostics", diagnostics);
        return Collections.unmodifiableMap(out);
    }

    private static List<String> safeList(List<String> source) {
        if (source == null || source.isEmpty()) {
            return List.of();
        }
        return source.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(item -> !item.isBlank())
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
