package com.tcc.pjb.backend.core.eleitoral.domain;

public record EleitoralZonaHealthQuery(
        String referencia,
        String criterio,
        java.time.Instant requestedAt
) {
}
