package com.tcc.pjb.backend.core.criminal.custodia.domain;

public record CustodiaPrazoWindowQuery(
        String referencia,
        String criterio,
        java.time.Instant requestedAt
) {
}
