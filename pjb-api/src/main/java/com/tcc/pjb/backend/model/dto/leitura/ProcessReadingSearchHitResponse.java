package com.tcc.pjb.backend.model.dto.leitura;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record ProcessReadingSearchHitResponse(
        UUID documentoId,
        String pageId,
        int pageNumber,
        String tituloDocumento,
        String fragment,
        String lane,
        String endpoint,
        String sourceType,
        String sourceId,
        String sourceLabel,
        @Schema(description = "Metadados tecnicos do hit de busca (relevancia, highlights)")
        @Size(max = 30)
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        Map<String, Object> metadata
) {
    public ProcessReadingSearchHitResponse {
        metadata = metadata == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(metadata));
    }
}

