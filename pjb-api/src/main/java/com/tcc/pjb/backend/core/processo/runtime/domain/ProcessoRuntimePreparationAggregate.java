package com.tcc.pjb.backend.core.processo.runtime.domain;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record ProcessoRuntimePreparationAggregate(
        ProcessoRuntimeContext context,
        ProcessoRuntimeIntegrationStatus integrationStatus,
        Instant preparadoEm,
        List<String> alertas,
        String fingerprint
) {
    public ProcessoRuntimePreparationAggregate {
        context = Objects.requireNonNull(context);
        integrationStatus = Objects.requireNonNull(integrationStatus);
        preparadoEm = preparadoEm == null ? Instant.now() : preparadoEm;
        alertas = alertas == null ? List.of() : List.copyOf(alertas);
        fingerprint = fingerprint == null ? "" : fingerprint.trim();
    }

    public boolean prontoParaMalhaCompleta() {
        return integrationStatus.prontoMinimo() && alertas.stream().noneMatch(alerta -> alerta.equalsIgnoreCase("runtime-incompleto"));
    }
}
