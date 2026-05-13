package com.tcc.pjb.backend.core.icp.domain;

public record IcpOcspWindowResult(
        boolean ok,
        String mensagem,
        java.time.Instant processedAt
) {
}
