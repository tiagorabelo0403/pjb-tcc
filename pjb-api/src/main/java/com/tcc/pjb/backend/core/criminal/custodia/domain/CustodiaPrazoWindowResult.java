package com.tcc.pjb.backend.core.criminal.custodia.domain;

public record CustodiaPrazoWindowResult(
        boolean ok,
        String mensagem,
        java.time.Instant processedAt
) {
}
