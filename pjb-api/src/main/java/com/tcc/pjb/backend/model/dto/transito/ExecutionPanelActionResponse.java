package com.tcc.pjb.backend.model.dto.transito;

import java.util.Map;

public record ExecutionPanelActionResponse(
        String action,
        String label,
        String severity,
        boolean enabled,
        String endpoint,
        Map<String, Object> payload
) {
    public ExecutionPanelActionResponse {
        payload = payload == null ? Map.of() : Map.copyOf(payload);
    }
}
