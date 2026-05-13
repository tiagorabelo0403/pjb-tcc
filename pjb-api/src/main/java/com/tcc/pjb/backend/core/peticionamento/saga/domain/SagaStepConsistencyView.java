package com.tcc.pjb.backend.core.peticionamento.saga.domain;

public record SagaStepConsistencyView(
        String referencia,
        String status,
        java.time.Instant updatedAt
) {
}
