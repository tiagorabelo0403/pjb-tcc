package com.tcc.pjb.backend.core.prazos.auditoria.domain;

public record PrazoPolicyAuditView(
        String referencia,
        String status,
        java.time.Instant updatedAt
) {
}
