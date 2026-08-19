package com.tcc.pjb.backend.modules.custas.domain;

public record CustaEmissionHealthQuery(
        String referencia,
        String criterio,
        java.time.Instant requestedAt
) {
}
