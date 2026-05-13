package com.tcc.pjb.backend.platform.security.idempotency.domain;

public record PjbIdempotencyExecutionHealthResult(
        boolean ok,
        String mensagem,
        java.time.Instant processedAt
) {
}
