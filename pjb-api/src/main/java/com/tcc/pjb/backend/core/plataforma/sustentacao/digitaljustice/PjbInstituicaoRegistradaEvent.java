package com.tcc.pjb.backend.core.plataforma.sustentacao.digitaljustice;

import java.time.Instant;

public record PjbInstituicaoRegistradaEvent(
        String tribunalCode,
        String comarcaCode,
        String institutionName,
        boolean firstLogin,
        Instant occurredAt
) {

    public PjbInstituicaoRegistradaEvent {
        occurredAt = occurredAt == null ? Instant.now() : occurredAt;
    }
}
