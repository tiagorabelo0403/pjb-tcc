package com.tcc.pjb.backend.platform.security.idempotency.domain;

public record PjbIdempotencyReplayHealthResult(
        boolean available,
        String summary,
        Long total
) {
}
