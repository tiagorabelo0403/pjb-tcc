package com.tcc.pjb.backend.core.processo.hardening.domain;

import java.util.List;
import java.util.Objects;

public record ProcessoHardeningFinding(
        String code,
        String axis,
        String severity,
        boolean blocking,
        String message,
        List<String> correctiveActions
) {
    public ProcessoHardeningFinding {
        Objects.requireNonNull(code);
        axis = axis == null ? "GERAL" : axis;
        severity = severity == null ? "CONTROLADA" : severity;
        message = message == null ? "" : message;
        correctiveActions = correctiveActions == null ? List.of() : List.copyOf(correctiveActions);
    }

    public String detail() {
        return message();
    }
}
