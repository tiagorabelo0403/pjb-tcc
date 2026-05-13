package com.tcc.pjb.backend.core.digitalizacao.domain;

public record DigitalizacaoEngineHealthResult(
        boolean ok,
        String mensagem,
        java.time.Instant processedAt
) {
}
