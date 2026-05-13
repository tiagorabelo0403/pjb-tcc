package com.tcc.pjb.backend.core.financeiro.custas.domain;

public record CustaEmissionHealthResult(
        boolean ok,
        String mensagem,
        java.time.Instant processedAt
) {
}
