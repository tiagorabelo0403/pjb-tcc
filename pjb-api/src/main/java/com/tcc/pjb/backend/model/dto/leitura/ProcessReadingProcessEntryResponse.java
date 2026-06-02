package com.tcc.pjb.backend.model.dto.leitura;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

public record ProcessReadingProcessEntryResponse(
        String entryId,
        String sourceType,
        String originMode,
        String title,
        String bodyPreview,
        String actor,
        @Schema(description = "Data/hora em que o evento processual ocorreu", format = "date-time",
                example = "2026-06-01T10:00:00-03:00") String occurredAt,
        String lane,
        String severity,
        boolean downloadable,
        String readerEndpoint,
        String pdfEndpoint,
        List<String> tags,
        @Schema(description = "Metadados tecnicos da entry processual (processoId, entryId, contentEndpoint)")
        @Size(max = 30)
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        Map<String, Object> metadata
) {
    public ProcessReadingProcessEntryResponse {
        tags = tags == null ? List.of() : List.copyOf(tags);
        metadata = metadata == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(metadata));
    }
}

