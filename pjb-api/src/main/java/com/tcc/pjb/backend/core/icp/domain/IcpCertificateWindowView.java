package com.tcc.pjb.backend.core.icp.domain;

public record IcpCertificateWindowView(
        String referencia,
        String status,
        java.time.Instant updatedAt
) {
}
