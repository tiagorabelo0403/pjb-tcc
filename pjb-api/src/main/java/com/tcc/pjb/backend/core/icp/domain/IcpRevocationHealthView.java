package com.tcc.pjb.backend.core.icp.domain;

public record IcpRevocationHealthView(
        String referencia,
        String status,
        java.time.Instant updatedAt
) {
}
