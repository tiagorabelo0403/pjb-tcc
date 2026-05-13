package com.tcc.pjb.backend.core.icp.domain;

public record IcpSignerHealthResult(
        boolean ok,
        String mensagem,
        java.time.Instant processedAt
) {
}
