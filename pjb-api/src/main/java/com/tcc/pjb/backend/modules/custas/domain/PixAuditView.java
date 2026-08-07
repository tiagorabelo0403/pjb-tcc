package com.tcc.pjb.backend.modules.custas.domain;

public record PixAuditView(
        String referencia,
        String status,
        java.time.Instant updatedAt
) {
}
