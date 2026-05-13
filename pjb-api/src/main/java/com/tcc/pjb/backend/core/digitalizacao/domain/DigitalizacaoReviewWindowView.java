package com.tcc.pjb.backend.core.digitalizacao.domain;

public record DigitalizacaoReviewWindowView(
        String referencia,
        String status,
        java.time.Instant updatedAt
) {
}
