package com.tcc.pjb.backend.core.criminal.custodia.domain;

public record CustodiaMedidaHealthResult(
        boolean ok,
        String mensagem,
        java.time.Instant processedAt
) {
}
