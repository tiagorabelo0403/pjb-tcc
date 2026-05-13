package com.tcc.pjb.backend.core.processo.unificado.domain;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record ProcessoUnificadoDiagnostico(
        boolean healthy,
        long totalFindings,
        long blockingFindings,
        long atosPermitidos,
        long atosBloqueados,
        long atosSensiveis,
        long atosComSegurancaElevada,
        List<ProcessoUnificadoFinding> findings,
        List<String> fundamentos,
        Instant generatedAt
) {
    public ProcessoUnificadoDiagnostico {
        Objects.requireNonNull(findings);
        Objects.requireNonNull(fundamentos);
        Objects.requireNonNull(generatedAt);
        findings = List.copyOf(findings);
        fundamentos = List.copyOf(fundamentos);
    }
}
