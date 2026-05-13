package com.tcc.pjb.backend.core.events;

import java.time.Instant;
import java.util.Objects;

public record PrazoVencidoEvent(
        Long processoId,
        String prazoId,
        String tipoPrazo,
        String responsavelCpfCnpj,
        String unidadeJudicial,
        Instant venceuEm,
        Instant occurredAt
) {
    public PrazoVencidoEvent {
        Objects.requireNonNull(processoId, "processoId");
        Objects.requireNonNull(prazoId, "prazoId");
        Objects.requireNonNull(tipoPrazo, "tipoPrazo");
        venceuEm = venceuEm == null ? Instant.now() : venceuEm;
        occurredAt = occurredAt == null ? Instant.now() : occurredAt;
    }
}
