package com.tcc.pjb.backend.modules.custas.domain;

public record CustaEmissionHealthResult(
        boolean ok,
        String mensagem,
        java.time.Instant processedAt
) {
}
