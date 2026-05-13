package com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain;

import java.util.List;
import java.util.Objects;

public record InstitutionalSessionRiskFinding(
        String code,
        InstitutionalRiskSeverity severity,
        boolean blocking,
        String message,
        List<String> evidences
) {
    public InstitutionalSessionRiskFinding {
        Objects.requireNonNull(code);
        Objects.requireNonNull(severity);
        Objects.requireNonNull(message);
        evidences = evidences == null ? List.of() : List.copyOf(evidences);
    }
}
