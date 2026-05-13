package com.tcc.pjb.backend.integration.judicial;

import java.time.Instant;
import java.util.Map;

public record NormalizedProcessEvent(
        JudicialSystem system,
        String numeroUnificado,
        String eventType,
        Instant occurredAt,
        String summary,
        Map<String, Object> evidence
) {
}
