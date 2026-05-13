package com.tcc.pjb.backend.integration.judicial.security;

import com.tcc.pjb.backend.integration.judicial.JudicialMapSupport;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record JudicialSecureHttpRequest(
        String method,
        Map<String, List<String>> headers,
        byte[] body,
        Duration requestTimeout,
        String operationName,
        String correlationId,
        Map<String, Object> metadata
) {
    public JudicialSecureHttpRequest {
        headers = copyHeaders(headers);
        body = body == null ? null : body.clone();
        metadata = JudicialMapSupport.copyNonNull(metadata);
    }

    public static JudicialSecureHttpRequest get(Map<String, List<String>> headers,
                                                Duration requestTimeout,
                                                String operationName,
                                                String correlationId,
                                                Map<String, Object> metadata) {
        return new JudicialSecureHttpRequest("GET", headers, null, requestTimeout, operationName, correlationId, metadata);
    }

    public static JudicialSecureHttpRequest post(byte[] body,
                                                 Map<String, List<String>> headers,
                                                 Duration requestTimeout,
                                                 String operationName,
                                                 String correlationId,
                                                 Map<String, Object> metadata) {
        return new JudicialSecureHttpRequest("POST", headers, body, requestTimeout, operationName, correlationId, metadata);
    }

    private static Map<String, List<String>> copyHeaders(Map<String, List<String>> source) {
        if (source == null || source.isEmpty()) {
            return Map.of();
        }
        LinkedHashMap<String, List<String>> out = new LinkedHashMap<>();
        source.forEach((key, value) -> {
            if (key != null && !key.isBlank() && value != null && !value.isEmpty()) {
                ArrayList<String> items = new ArrayList<>();
                value.stream().filter(item -> item != null && !item.isBlank()).forEach(item -> items.add(item.trim()));
                if (!items.isEmpty()) {
                    out.put(key.trim(), List.copyOf(items));
                }
            }
        });
        return out.isEmpty() ? Map.of() : Map.copyOf(out);
    }
}
