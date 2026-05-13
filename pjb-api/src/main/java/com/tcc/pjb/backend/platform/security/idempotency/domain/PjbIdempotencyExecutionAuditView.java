package com.tcc.pjb.backend.platform.security.idempotency.domain;

public record PjbIdempotencyExecutionAuditView(
        String reference,
        String status,
        String summary
) {
}
