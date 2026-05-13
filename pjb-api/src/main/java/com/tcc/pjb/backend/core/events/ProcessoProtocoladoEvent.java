package com.tcc.pjb.backend.core.events;

import java.time.Instant;
import java.util.Objects;

public record ProcessoProtocoladoEvent(
        Long processoId,
        String numeroUnificado,
        String classeProcessual,
        String ramoDireito,
        String rito,
        String comarca,
        String uf,
        String tribunalCodigo,
        boolean juizo100Digital,
        Instant occurredAt
) {
    public ProcessoProtocoladoEvent {
        Objects.requireNonNull(processoId, "processoId");
        Objects.requireNonNull(numeroUnificado, "numeroUnificado");
        occurredAt = occurredAt == null ? Instant.now() : occurredAt;
    }
}
