package com.tcc.pjb.backend.core.processo.migracao.domain;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record ProcessoMigracaoAggregate(
        ProcessoMigracaoIdentity identity,
        String readiness,
        boolean canCutOver,
        List<ProcessoLegacyMirror> mirrors,
        List<ProcessoShadowComparison> comparacoes,
        List<String> proximasOndas,
        List<String> alertas,
        Instant geradoEm
) {
    public ProcessoMigracaoAggregate {
        Objects.requireNonNull(identity);
        readiness = readiness == null ? "NAO_AVALIADO" : readiness;
        mirrors = mirrors == null ? List.of() : List.copyOf(mirrors);
        comparacoes = comparacoes == null ? List.of() : List.copyOf(comparacoes);
        proximasOndas = proximasOndas == null ? List.of() : List.copyOf(proximasOndas);
        alertas = alertas == null ? List.of() : List.copyOf(alertas);
        geradoEm = geradoEm == null ? Instant.now() : geradoEm;
    }
}
