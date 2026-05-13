package com.tcc.pjb.backend.integration.mni.domain;

public record MniRoutingHealthResult(
        boolean ok,
        String mensagem,
        java.time.Instant processedAt
) {
}
