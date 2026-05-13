package com.tcc.pjb.backend.model.dto.integration.n8n;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.LinkedHashMap;
import java.util.Map;

public record N8nDispatchRequest(
        @NotBlank @Size(max = 120) String eventType,
        @NotBlank @Size(max = 160) String workflowKey,
        @NotBlank @Size(max = 80) String requestId,
        @Size(max = 80) String traceId,
        @Size(max = 80) String tenant,
        Map<String, Object> payload,
        Map<String, String> headers
) {
    public N8nDispatchRequest {
        payload = immutablePayload(payload);
        headers = immutableHeaders(headers);
    }

    private static Map<String, Object> immutablePayload(Map<String, Object> source) {
        if (source == null || source.isEmpty()) {
            return Map.of();
        }
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        source.forEach((key, value) -> {
            if (key != null && !key.isBlank() && value != null) {
                out.put(key, value);
            }
        });
        return out.isEmpty() ? Map.of() : Map.copyOf(out);
    }

    private static Map<String, String> immutableHeaders(Map<String, String> source) {
        if (source == null || source.isEmpty()) {
            return Map.of();
        }
        LinkedHashMap<String, String> out = new LinkedHashMap<>();
        source.forEach((key, value) -> {
            if (key != null && !key.isBlank() && value != null && !value.isBlank()) {
                out.put(key, value.trim());
            }
        });
        return out.isEmpty() ? Map.of() : Map.copyOf(out);
    }
}
