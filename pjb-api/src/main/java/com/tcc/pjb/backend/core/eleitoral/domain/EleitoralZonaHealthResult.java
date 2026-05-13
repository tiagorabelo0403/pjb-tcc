package com.tcc.pjb.backend.core.eleitoral.domain;

public record EleitoralZonaHealthResult(
        boolean ok,
        String mensagem,
        java.time.Instant processedAt
) {
}
