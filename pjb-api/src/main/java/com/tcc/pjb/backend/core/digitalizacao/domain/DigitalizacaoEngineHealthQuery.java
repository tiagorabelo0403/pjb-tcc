package com.tcc.pjb.backend.core.digitalizacao.domain;

public record DigitalizacaoEngineHealthQuery(
        String referencia,
        String criterio,
        java.time.Instant requestedAt
) {
}
