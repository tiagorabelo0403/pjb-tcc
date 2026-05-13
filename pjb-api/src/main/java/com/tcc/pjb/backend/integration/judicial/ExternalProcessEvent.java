package com.tcc.pjb.backend.integration.judicial;

import java.time.Instant;
import java.util.Map;

public record ExternalProcessEvent(
        JudicialSystem system,
        String numeroUnificado,
        String externalId,
        String type,
        String description,
        Instant occurredAt,
        Map<String, Object> raw
) {
}
