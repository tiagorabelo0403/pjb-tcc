package com.tcc.pjb.backend.core.eleitoral.domain;

public record EleitoralPrestacaoContasConsistencyView(
        String referencia,
        String status,
        java.time.Instant updatedAt
) {
}
