package com.tcc.pjb.backend.model.dto.distribuicao;

import java.util.Map;

public record DistributionWorkbenchActionResponse(
        String action,
        String label,
        String severity,
        boolean enabled,
        String endpoint,
        Map<String, Object> payload
) {
    public DistributionWorkbenchActionResponse {
        payload = payload == null ? Map.of() : Map.copyOf(payload);
    }
}
