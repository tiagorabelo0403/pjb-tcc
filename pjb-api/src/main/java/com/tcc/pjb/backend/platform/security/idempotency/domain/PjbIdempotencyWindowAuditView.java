package com.tcc.pjb.backend.platform.security.idempotency.domain;

public record PjbIdempotencyWindowAuditView(
        String reference,
        String status,
        String summary
) {
}
