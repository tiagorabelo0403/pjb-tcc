package com.tcc.pjb.backend.integration.datajud.feed.domain;

public record DataJudTribunalCursorAuditView(
        String referencia,
        String status,
        java.time.Instant updatedAt
) {
}
