package com.tcc.pjb.backend.core.comunicacao.institucional.coerencia.domain;

import java.util.List;
import java.util.Objects;

public record InstitutionalProceduralActEvaluation(
        String actionCode,
        String actionTitle,
        boolean allowed,
        boolean blocking,
        int coherenceScore,
        String decision,
        List<String> mandatoryGuards,
        List<InstitutionalProceduralCoherenceFinding> findings,
        List<String> fundamentos
) {
    public InstitutionalProceduralActEvaluation {
        Objects.requireNonNull(actionCode);
        Objects.requireNonNull(actionTitle);
        Objects.requireNonNull(decision);
        mandatoryGuards = mandatoryGuards == null ? List.of() : List.copyOf(mandatoryGuards);
        findings = findings == null ? List.of() : List.copyOf(findings);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
    }
}
