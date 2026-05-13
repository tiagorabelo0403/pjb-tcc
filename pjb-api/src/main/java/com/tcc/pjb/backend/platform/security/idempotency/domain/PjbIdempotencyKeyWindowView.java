package com.tcc.pjb.backend.platform.security.idempotency.domain;

public record PjbIdempotencyKeyWindowView(
        String reference,
        String status,
        String summary
) {
}
