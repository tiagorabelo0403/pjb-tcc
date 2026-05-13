package com.tcc.pjb.backend.core.dje.domain;

public record DjeTribunalHealthResult(
        boolean ok,
        String mensagem,
        java.time.Instant processedAt
) {
}
