package com.tcc.pjb.backend.core.financeiro.custas.domain;

public record PixAuditView(
        String referencia,
        String status,
        java.time.Instant updatedAt
) {
}
