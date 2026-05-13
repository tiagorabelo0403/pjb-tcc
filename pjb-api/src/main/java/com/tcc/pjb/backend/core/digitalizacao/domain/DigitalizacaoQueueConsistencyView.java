package com.tcc.pjb.backend.core.digitalizacao.domain;

public record DigitalizacaoQueueConsistencyView(
        String referencia,
        String status,
        java.time.Instant updatedAt
) {
}
