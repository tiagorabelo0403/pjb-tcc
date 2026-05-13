package com.tcc.pjb.backend.integration.mni.domain;

public record MniProtocolAuditView(
        String referencia,
        String status,
        java.time.Instant updatedAt
) {
}
