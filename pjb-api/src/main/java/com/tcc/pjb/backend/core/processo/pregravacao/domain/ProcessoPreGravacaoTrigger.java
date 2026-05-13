package com.tcc.pjb.backend.core.processo.pregravacao.domain;

import java.util.List;
import java.util.Objects;

public record ProcessoPreGravacaoTrigger(
        String code,
        String sourceAxis,
        String severity,
        boolean blocking,
        boolean stepUpRequired,
        String summary,
        String rationale,
        List<String> evidences,
        List<String> correctiveActions
) {
    public ProcessoPreGravacaoTrigger {
        Objects.requireNonNull(code);
        sourceAxis = sourceAxis == null ? "GERAL" : sourceAxis;
        severity = severity == null ? "CONTROLADA" : severity;
        Objects.requireNonNull(summary);
        rationale = rationale == null ? "" : rationale;
        evidences = evidences == null ? List.of() : List.copyOf(evidences);
        correctiveActions = correctiveActions == null ? List.of() : List.copyOf(correctiveActions);
    }
}
