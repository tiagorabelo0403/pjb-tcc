package com.tcc.pjb.backend.core.observability.domain;

public record PjbSloRegistryAuditView(
        String referencia,
        String status,
        java.time.Instant updatedAt
) {
}
