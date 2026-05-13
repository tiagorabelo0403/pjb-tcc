package com.tcc.pjb.backend.core.peticionamento.saga.domain;

public record SagaLifecycleHealthResult(
        boolean ok,
        String mensagem,
        java.time.Instant processedAt
) {
}
