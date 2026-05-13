package com.tcc.pjb.backend.core.processo.hardening.domain;

import com.tcc.pjb.backend.core.processo.unificado.domain.ProcessoUnificadoIdentity;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record ProcessoHardeningAggregate(
        ProcessoUnificadoIdentity identity,
        String readiness,
        long hardeningScore,
        long blockingFindings,
        long totalFindings,
        List<String> hardeningAxes,
        List<ProcessoHardeningFinding> findings,
        List<String> correctivePlan,
        List<String> fundamentos,
        Instant generatedAt
) {
    public ProcessoHardeningAggregate {
        Objects.requireNonNull(identity);
        readiness = readiness == null ? "NOT_READY" : readiness;
        hardeningAxes = hardeningAxes == null ? List.of() : List.copyOf(hardeningAxes);
        findings = findings == null ? List.of() : List.copyOf(findings);
        correctivePlan = correctivePlan == null ? List.of() : List.copyOf(correctivePlan);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
        generatedAt = generatedAt == null ? Instant.now() : generatedAt;
    }
}
