package com.tcc.pjb.backend.core.comunicacao.institucional.coerencia.domain;

import java.util.List;
import java.util.Objects;

public record InstitutionalProceduralNextBestAct(
        String actionCode,
        String actionTitle,
        int priorityScore,
        String rationale,
        List<String> expectedGuards,
        List<String> fundamentos
) {
    public InstitutionalProceduralNextBestAct {
        Objects.requireNonNull(actionCode);
        Objects.requireNonNull(actionTitle);
        Objects.requireNonNull(rationale);
        expectedGuards = expectedGuards == null ? List.of() : List.copyOf(expectedGuards);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
    }
}
