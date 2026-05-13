package com.tcc.pjb.backend.model.dto.integration.n8n;

import java.time.Instant;

public record N8nDispatchResponse(
        boolean accepted,
        int statusCode,
        String requestId,
        String traceId,
        String endpoint,
        Instant dispatchedAt,
        String payloadHash,
        String responseExcerpt
) {
}
