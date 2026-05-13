package com.tcc.pjb.backend.platform.security.idempotency.domain;

import java.time.Instant;

public record PjbIdempotencyReplayPayload(
        int status,
        String contentType,
        String body,
        String location,
        Instant completedAt
) {
}
