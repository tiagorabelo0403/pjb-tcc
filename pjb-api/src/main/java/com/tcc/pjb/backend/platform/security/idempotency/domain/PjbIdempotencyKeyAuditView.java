package com.tcc.pjb.backend.platform.security.idempotency.domain;

public record PjbIdempotencyKeyAuditView(
        String referencia,
        String status,
        java.time.Instant updatedAt
) {
}
