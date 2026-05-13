package com.tcc.pjb.backend.core.dje.domain;

public record DjePrazoWindowResult(
        boolean ok,
        String mensagem,
        java.time.Instant processedAt
) {
}
