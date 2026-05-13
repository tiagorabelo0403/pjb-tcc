package com.tcc.pjb.backend.core.events;

import java.time.Instant;
import java.util.Objects;

public record TeseVinculanteDecididaEvent(
        String teseVinculanteId,
        String tribunalOrigem,
        String ementa,
        String resultado,
        boolean aplicacaoImediata,
        Instant julgadaEm,
        Instant occurredAt
) {
    public TeseVinculanteDecididaEvent {
        Objects.requireNonNull(teseVinculanteId, "teseVinculanteId");
        Objects.requireNonNull(tribunalOrigem, "tribunalOrigem");
        Objects.requireNonNull(resultado, "resultado");
        julgadaEm = julgadaEm == null ? Instant.now() : julgadaEm;
        occurredAt = occurredAt == null ? Instant.now() : occurredAt;
    }
}
