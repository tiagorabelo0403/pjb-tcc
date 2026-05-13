package com.tcc.pjb.backend.core.dje.domain;

public record DjeTribunalHealthQuery(
        String referencia,
        String criterio,
        java.time.Instant requestedAt
) {
}
