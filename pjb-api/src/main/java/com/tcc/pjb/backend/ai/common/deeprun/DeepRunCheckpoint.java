package com.tcc.pjb.backend.ai.common.deeprun;

import java.time.Instant;
import java.util.Map;

public record DeepRunCheckpoint(
        Instant at,
        String summary,
        Map<String, Object> metrics
) {
}
