package com.tcc.pjb.backend.integration.mni.domain;

public record MniRoutingHealthQuery(
        String referencia,
        String criterio,
        java.time.Instant requestedAt
) {
}
