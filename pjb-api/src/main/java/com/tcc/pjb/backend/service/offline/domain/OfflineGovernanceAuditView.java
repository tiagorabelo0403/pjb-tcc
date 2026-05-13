package com.tcc.pjb.backend.service.offline.domain;

public record OfflineGovernanceAuditView(
        String referencia,
        String status,
        java.time.Instant updatedAt
) {
}
