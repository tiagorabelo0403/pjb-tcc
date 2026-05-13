package com.tcc.pjb.backend.core.observability.domain;

public record PjbSloWindowHealthView(
        String referencia,
        String status,
        java.time.Instant updatedAt
) {
}
