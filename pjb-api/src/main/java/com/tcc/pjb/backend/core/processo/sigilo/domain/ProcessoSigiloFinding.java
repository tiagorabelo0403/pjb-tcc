package com.tcc.pjb.backend.core.processo.sigilo.domain;

import java.util.List;
import java.util.Objects;

public record ProcessoSigiloFinding(
        String code,
        String title,
        String severity,
        boolean blocking,
        String detail,
        List<String> correctiveActions
) {
    public ProcessoSigiloFinding {
        Objects.requireNonNull(code);
        Objects.requireNonNull(title);
        severity = severity == null ? "CONTROLADA" : severity;
        detail = detail == null ? "" : detail;
        correctiveActions = correctiveActions == null ? List.of() : List.copyOf(correctiveActions);
    }
}
