package com.tcc.pjb.backend.core.events;

import java.time.Instant;
import java.util.Objects;

public record ProcessoDistribuidoEvent(
        Long processoId,
        String numeroUnificado,
        String juizNatural,
        String unidadeJudicial,
        String estrategiaDistribuicao,
        String trilhaExplicativa,
        Instant occurredAt
) {
    public ProcessoDistribuidoEvent {
        Objects.requireNonNull(processoId, "processoId");
        Objects.requireNonNull(unidadeJudicial, "unidadeJudicial");
        occurredAt = occurredAt == null ? Instant.now() : occurredAt;
    }
}
