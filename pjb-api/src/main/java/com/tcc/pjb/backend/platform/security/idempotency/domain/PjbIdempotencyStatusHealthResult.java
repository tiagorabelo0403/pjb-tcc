package com.tcc.pjb.backend.platform.security.idempotency.domain;

public record PjbIdempotencyStatusHealthResult(
        boolean available,
        String summary,
        Long total
) {
}
