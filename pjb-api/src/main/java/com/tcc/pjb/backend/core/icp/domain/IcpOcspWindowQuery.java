package com.tcc.pjb.backend.core.icp.domain;

public record IcpOcspWindowQuery(
        String referencia,
        String criterio,
        java.time.Instant requestedAt
) {
}
