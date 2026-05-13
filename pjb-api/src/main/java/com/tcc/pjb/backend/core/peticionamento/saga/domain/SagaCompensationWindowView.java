package com.tcc.pjb.backend.core.peticionamento.saga.domain;

public record SagaCompensationWindowView(
        String referencia,
        String status,
        java.time.Instant updatedAt
) {
}
