package com.tcc.pjb.backend.core.observability.domain;

public record PjbSloOperationAuditView(
        String referencia,
        String status,
        java.time.Instant updatedAt
) {
}
