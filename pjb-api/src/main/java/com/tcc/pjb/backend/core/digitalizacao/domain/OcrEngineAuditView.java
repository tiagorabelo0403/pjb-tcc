package com.tcc.pjb.backend.core.digitalizacao.domain;

public record OcrEngineAuditView(
        String referencia,
        String status,
        java.time.Instant updatedAt
) {
}
