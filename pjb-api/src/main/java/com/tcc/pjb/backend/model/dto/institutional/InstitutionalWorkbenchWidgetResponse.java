package com.tcc.pjb.backend.model.dto.institutional;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

public record InstitutionalWorkbenchWidgetResponse(
        String code,
        String title,
        String kind,
        boolean enabled,
        int priority,
        String route,
        String summary,
        @Schema(description = "Payload de widget institucional — polimorfico por tipo e configuracao de widget", implementation = Object.class)
        @Size(max = 30)
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        Map<String, Object> payload,
        List<String> warnings
) {
    public InstitutionalWorkbenchWidgetResponse {
        payload = immutablePayload(payload);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }

    private static Map<String, Object> immutablePayload(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            return Map.of();
        }
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        payload.forEach((key, value) -> {
            if (key != null && !key.isBlank()) {
                out.put(key, value);
            }
        });
        return Collections.unmodifiableMap(new LinkedHashMap<>(out));
    }
}

