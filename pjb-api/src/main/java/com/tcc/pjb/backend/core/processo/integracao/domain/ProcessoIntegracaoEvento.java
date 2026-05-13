package com.tcc.pjb.backend.core.processo.integracao.domain;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record ProcessoIntegracaoEvento(
        String codigo,
        String titulo,
        String eixo,
        String status,
        Instant occurredAt,
        String correlationKey,
        List<String> details
) {
    public ProcessoIntegracaoEvento {
        Objects.requireNonNull(codigo);
        Objects.requireNonNull(titulo);
        eixo = eixo == null ? "INTEGRACAO" : eixo;
        status = status == null ? "NAO_INFORMADO" : status;
        correlationKey = correlationKey == null ? "SEM_CORRELACAO" : correlationKey;
        details = details == null ? List.of() : List.copyOf(details);
    }
}
