package com.tcc.pjb.backend.core.dje.domain;

public record DjeNotificationAuditView(
        String referencia,
        String status,
        java.time.Instant updatedAt
) {
}
