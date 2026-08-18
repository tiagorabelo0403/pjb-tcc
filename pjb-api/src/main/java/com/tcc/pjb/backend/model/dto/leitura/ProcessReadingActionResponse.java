package com.tcc.pjb.backend.model.dto.leitura;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

public record ProcessReadingActionResponse(
        String action,
        String label,
        String severity,
        boolean enabled,
        String endpoint,
        @Schema(description = "Payload da acao de leitura — estrutura varia por tipo de acao (NOVA_PETICAO, ASSINAR, etc.)")
        @Size(max = 30)
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        Map<String, Object> payload
) {
    public ProcessReadingActionResponse {
        payload = payload == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(payload));
    }
}

