package com.tcc.pjb.backend.core.processo.papel.domain;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record ProcessoPapelAggregate(
        ProcessoPapelIdentity identity,
        long totalPerfis,
        long totalAssinantes,
        long totalRecursais,
        long totalCertificadores,
        List<ProcessoPapelPerfil> perfis,
        List<String> alertas,
        Instant geradoEm
) {
    public ProcessoPapelAggregate {
        Objects.requireNonNull(identity);
        perfis = perfis == null ? List.of() : List.copyOf(perfis);
        alertas = alertas == null ? List.of() : List.copyOf(alertas);
        geradoEm = geradoEm == null ? Instant.now() : geradoEm;
    }
}
