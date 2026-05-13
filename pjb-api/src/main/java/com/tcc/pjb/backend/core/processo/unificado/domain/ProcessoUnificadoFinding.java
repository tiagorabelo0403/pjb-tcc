package com.tcc.pjb.backend.core.processo.unificado.domain;

import java.util.Objects;

public record ProcessoUnificadoFinding(
        String code,
        String severity,
        boolean blocking,
        String title,
        String detail
) {
    public ProcessoUnificadoFinding {
        Objects.requireNonNull(code);
        Objects.requireNonNull(severity);
        Objects.requireNonNull(title);
        Objects.requireNonNull(detail);
    }
}
