package com.tcc.pjb.backend.service.semantic;

import java.util.Map;

public record VectorSearchHit(
        String id,
        float score,
        Map<String, String> metadata
) {
}
