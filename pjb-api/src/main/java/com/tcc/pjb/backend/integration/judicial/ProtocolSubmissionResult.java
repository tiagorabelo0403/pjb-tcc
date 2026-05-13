package com.tcc.pjb.backend.integration.judicial;

import java.time.Instant;
import java.util.Map;

public record ProtocolSubmissionResult(
        boolean accepted,
        JudicialSystem system,
        String protocolReference,
        String status,
        String message,
        Instant processedAt,
        Map<String, Object> raw
) {
    public ProtocolSubmissionResult {
        processedAt = processedAt == null ? Instant.now() : processedAt;
        raw = JudicialMapSupport.copyNonNull(raw);
    }
}
