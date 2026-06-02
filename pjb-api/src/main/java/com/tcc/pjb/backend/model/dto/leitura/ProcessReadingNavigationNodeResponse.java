package com.tcc.pjb.backend.model.dto.leitura;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record ProcessReadingNavigationNodeResponse(
        UUID documentoId,
        String pageId,
        int pageNumber,
        String nodeType,
        String label,
        int priority,
        String fragment,
        String endpoint,
        @Schema(description = "Metadados tecnicos do no de navegacao — varia por tipo de documento")
        @Size(max = 30)
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        Map<String, Object> metadata
) {
    public ProcessReadingNavigationNodeResponse {
        metadata = metadata == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(metadata));
    }
}

