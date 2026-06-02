package com.tcc.pjb.backend.model.dto.leitura;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

public record ProcessReadingLaneResponse(
        String code,
        String status,
        String descriptor,
        List<String> highlights,
        @Schema(description = "Metadados tecnicos da lane de leitura — varia por tipo de lane")
        @Size(max = 30)
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        Map<String, Object> metadata
) {
    public ProcessReadingLaneResponse {
        highlights = highlights == null ? List.of() : List.copyOf(highlights);
        metadata = metadata == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(metadata));
    }
}

