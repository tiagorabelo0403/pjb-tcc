package com.tcc.pjb.backend.integration.mni.domain;

public record MniRemessaConsistencyView(
        String referencia,
        String status,
        java.time.Instant updatedAt
) {
}
