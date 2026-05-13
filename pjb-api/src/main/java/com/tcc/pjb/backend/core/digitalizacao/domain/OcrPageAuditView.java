package com.tcc.pjb.backend.core.digitalizacao.domain;

public record OcrPageAuditView(
        String referencia,
        String status,
        java.time.Instant updatedAt
) {
}
