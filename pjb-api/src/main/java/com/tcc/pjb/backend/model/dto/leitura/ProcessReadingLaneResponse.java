package com.tcc.pjb.backend.model.dto.leitura;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record ProcessReadingLaneResponse(
        String code,
        String status,
        String descriptor,
        List<String> highlights,
        Map<String, Object> metadata
) {
    public ProcessReadingLaneResponse {
        highlights = highlights == null ? List.of() : List.copyOf(highlights);
        metadata = metadata == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(metadata));
    }
}
