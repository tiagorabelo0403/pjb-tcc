package com.tcc.pjb.backend.core.financeiro.custas.domain;

public record CustaEmissionHealthQuery(
        String referencia,
        String criterio,
        java.time.Instant requestedAt
) {
}
