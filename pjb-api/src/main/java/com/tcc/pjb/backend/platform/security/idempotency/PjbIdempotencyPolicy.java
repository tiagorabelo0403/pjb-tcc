package com.tcc.pjb.backend.platform.security.idempotency;

import java.time.Duration;

public record PjbIdempotencyPolicy(Duration ttl, int retryAfterSeconds) {

    public static PjbIdempotencyPolicy strict() {
        return new PjbIdempotencyPolicy(Duration.ofHours(24), 5);
    }
}
