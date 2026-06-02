package com.tcc.pjb.backend.model.dto.transito;

import java.util.Map;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

public record ExecutionPanelActionResponse(
        String action,
        String label,
        String severity,
        boolean enabled,
        String endpoint,
        @Schema(description = "Payload da acao judicial em transito — polimórfico por tipo de ato processual", implementation = Object.class)
        @Size(max = 30)
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        Map<String, Object> payload
) {
    public ExecutionPanelActionResponse {
        payload = payload == null ? Map.of() : Map.copyOf(payload);
    }
}

