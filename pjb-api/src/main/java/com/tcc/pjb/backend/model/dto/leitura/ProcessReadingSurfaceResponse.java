package com.tcc.pjb.backend.model.dto.leitura;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

public record ProcessReadingSurfaceResponse(
        String readerType,
        String sourceType,
        String displayMode,
        String extractionMode,
        String selectionMode,
        String ocrStatus,
        String preservationMode,
        String timelineMode,
        String contentEndpoint,
        String pdfEndpoint,
        String downloadEndpoint,
        List<String> markers,
        @Schema(description = "Metadados tecnicos da surface de leitura — varia por tipo")
        @Size(max = 30)
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        Map<String, Object> metadata
) {
    public ProcessReadingSurfaceResponse {
        markers = markers == null ? List.of() : List.copyOf(markers);
        metadata = metadata == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(metadata));
    }
}

