package com.tcc.pjb.backend.integration.mni.domain;

public record MniRecepcaoConsistencyView(
        String referencia,
        String status,
        java.time.Instant updatedAt
) {
}
