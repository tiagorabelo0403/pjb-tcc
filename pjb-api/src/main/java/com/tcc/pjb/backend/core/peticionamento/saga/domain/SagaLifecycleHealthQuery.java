package com.tcc.pjb.backend.core.peticionamento.saga.domain;

public record SagaLifecycleHealthQuery(
        String referencia,
        String criterio,
        java.time.Instant requestedAt
) {
}
