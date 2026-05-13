package com.tcc.pjb.backend.model.dto.leitura;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record ProcessReadingActionResponse(
        String action,
        String label,
        String severity,
        boolean enabled,
        String endpoint,
        Map<String, Object> payload
) {
    public ProcessReadingActionResponse {
        payload = payload == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(payload));
    }
}
