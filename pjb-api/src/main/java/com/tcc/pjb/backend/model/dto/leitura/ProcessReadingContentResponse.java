package com.tcc.pjb.backend.model.dto.leitura;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record ProcessReadingContentResponse(
        String readerId,
        String readerType,
        String title,
        ProcessReadingSurfaceResponse surface,
        ProcessReadingPreferenceResponse preference,
        String chronologyMode,
        String defaultFocusMode,
        boolean copyEnabled,
        boolean downloadable,
        boolean searchable,
        List<ProcessReadingContentBlockResponse> blocks,
        Map<String, Object> metadata
) {
    public ProcessReadingContentResponse {
        blocks = blocks == null ? List.of() : List.copyOf(blocks);
        metadata = metadata == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(metadata));
    }
}
