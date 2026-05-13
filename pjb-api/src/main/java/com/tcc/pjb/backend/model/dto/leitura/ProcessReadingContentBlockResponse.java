package com.tcc.pjb.backend.model.dto.leitura;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record ProcessReadingContentBlockResponse(
        String blockId,
        String sourceType,
        String blockType,
        String title,
        String body,
        Integer pageNumber,
        String anchor,
        String importance,
        List<String> tags,
        Map<String, Object> metadata
) {
    public ProcessReadingContentBlockResponse {
        tags = tags == null ? List.of() : List.copyOf(tags);
        metadata = metadata == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(metadata));
    }
}
