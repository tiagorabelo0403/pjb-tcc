package com.tcc.pjb.backend.platform.security.idempotency.domain;

public record PjbIdempotencyExecutionStatusView(
        String reference,
        String status,
        String summary
) {
}
