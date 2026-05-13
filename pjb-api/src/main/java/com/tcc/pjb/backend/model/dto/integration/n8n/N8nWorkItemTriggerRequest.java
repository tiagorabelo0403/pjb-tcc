package com.tcc.pjb.backend.model.dto.integration.n8n;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.LinkedHashMap;
import java.util.Map;

public record N8nWorkItemTriggerRequest(
        @NotBlank @Size(max = 80) String requestId,
        @NotNull Long processoId,
        Boolean force,
        @Size(max = 64) String fase,
        @Size(max = 160) String workflowKey,
        Map<String, Object> context
) {
    public N8nWorkItemTriggerRequest {
        context = immutableContext(context);
    }

    private static Map<String, Object> immutableContext(Map<String, Object> source) {
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
}
