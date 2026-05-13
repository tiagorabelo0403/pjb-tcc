package com.tcc.pjb.backend.core.dje.domain;

public record DjePublicationConsistencyView(
        String referencia,
        String status,
        java.time.Instant updatedAt
) {
}
