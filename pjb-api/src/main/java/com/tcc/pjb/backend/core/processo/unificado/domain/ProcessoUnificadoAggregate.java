package com.tcc.pjb.backend.core.processo.unificado.domain;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record ProcessoUnificadoAggregate(
        ProcessoUnificadoIdentity identity,
        ProcessoUnificadoCompetencia competencia,
        ProcessoUnificadoDiagnostico diagnostico,
        List<ProcessoUnificadoAto> atosPermitidos,
        List<ProcessoUnificadoAto> atosBloqueados,
        List<String> proximoMelhorAto,
        Instant generatedAt
) {
    public ProcessoUnificadoAggregate {
        Objects.requireNonNull(identity);
        Objects.requireNonNull(competencia);
        Objects.requireNonNull(diagnostico);
        Objects.requireNonNull(generatedAt);
        atosPermitidos = atosPermitidos == null ? List.of() : List.copyOf(atosPermitidos);
        atosBloqueados = atosBloqueados == null ? List.of() : List.copyOf(atosBloqueados);
        proximoMelhorAto = proximoMelhorAto == null ? List.of() : List.copyOf(proximoMelhorAto);
    }
}
