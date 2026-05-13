package com.tcc.pjb.backend.platform.security.idempotency.domain;

public record PjbIdempotencyExecutionHealthQuery(
        String referencia,
        String criterio,
        java.time.Instant requestedAt
) {
}
