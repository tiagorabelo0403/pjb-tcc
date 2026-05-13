package com.tcc.pjb.backend.model.dto.leitura;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record ProcessReadingProcessEntryResponse(
        String entryId,
        String sourceType,
        String originMode,
        String title,
        String bodyPreview,
        String actor,
        String occurredAt,
        String lane,
        String severity,
        boolean downloadable,
        String readerEndpoint,
        String pdfEndpoint,
        List<String> tags,
        Map<String, Object> metadata
) {
    public ProcessReadingProcessEntryResponse {
        tags = tags == null ? List.of() : List.copyOf(tags);
        metadata = metadata == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(metadata));
    }
}
