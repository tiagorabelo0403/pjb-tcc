package com.tcc.pjb.backend.model.dto.intelligence;

import java.time.Instant;
import java.util.Map;

public record MinutaPreviewResponse(
        String previewId,
        Instant generatedAt,
        String content,
        Map<String, Object> debug
) {
}
