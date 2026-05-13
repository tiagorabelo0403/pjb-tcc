package com.tcc.pjb.backend.core.icp.domain;

public record IcpSignerHealthQuery(
        String referencia,
        String criterio,
        java.time.Instant requestedAt
) {
}
