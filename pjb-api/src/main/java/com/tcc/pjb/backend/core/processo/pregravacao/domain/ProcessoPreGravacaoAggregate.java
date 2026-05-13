package com.tcc.pjb.backend.core.processo.pregravacao.domain;

import com.tcc.pjb.backend.core.processo.unificado.domain.ProcessoUnificadoIdentity;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record ProcessoPreGravacaoAggregate(
        ProcessoUnificadoIdentity identity,
        String profileCode,
        String actionCode,
        boolean persistenciaPermitida,
        long totalTriggers,
        long blockingTriggers,
        long stepUpTriggers,
        long mandatoryGuardCount,
        List<String> mandatoryGuards,
        List<ProcessoPreGravacaoTrigger> triggers,
        List<String> correctivePlan,
        List<String> fundamentos,
        Instant generatedAt
) {
    public ProcessoPreGravacaoAggregate {
        Objects.requireNonNull(identity);
        Objects.requireNonNull(profileCode);
        Objects.requireNonNull(actionCode);
        mandatoryGuards = mandatoryGuards == null ? List.of() : List.copyOf(mandatoryGuards);
        triggers = triggers == null ? List.of() : List.copyOf(triggers);
        correctivePlan = correctivePlan == null ? List.of() : List.copyOf(correctivePlan);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
        generatedAt = generatedAt == null ? Instant.now() : generatedAt;
    }
}
