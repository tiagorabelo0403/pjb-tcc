package com.tcc.pjb.backend.core.dje.domain;

public record DjePrazoWindowQuery(
        String referencia,
        String criterio,
        java.time.Instant requestedAt
) {
}
