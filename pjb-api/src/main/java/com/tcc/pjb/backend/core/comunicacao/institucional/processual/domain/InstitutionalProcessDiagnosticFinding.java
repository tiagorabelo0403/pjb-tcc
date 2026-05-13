package com.tcc.pjb.backend.core.comunicacao.institucional.processual.domain;

import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalRiskSeverity;
import java.util.List;
import java.util.Objects;

public record InstitutionalProcessDiagnosticFinding(
        String code,
        InstitutionalRiskSeverity severity,
        boolean blocking,
        String profileCode,
        String message,
        List<String> evidences
) {
    public InstitutionalProcessDiagnosticFinding {
        Objects.requireNonNull(code);
        Objects.requireNonNull(severity);
        Objects.requireNonNull(profileCode);
        Objects.requireNonNull(message);
        evidences = evidences == null ? List.of() : List.copyOf(evidences);
    }
}
