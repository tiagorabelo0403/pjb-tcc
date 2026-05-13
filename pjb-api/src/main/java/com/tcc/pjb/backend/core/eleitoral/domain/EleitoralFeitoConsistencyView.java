package com.tcc.pjb.backend.core.eleitoral.domain;

public record EleitoralFeitoConsistencyView(
        String referencia,
        String status,
        java.time.Instant updatedAt
) {
}
