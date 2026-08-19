package com.tcc.pjb.backend.modules.custas.domain;

public record GruAuditView(
        String referencia,
        String status,
        java.time.Instant updatedAt
) {
}
