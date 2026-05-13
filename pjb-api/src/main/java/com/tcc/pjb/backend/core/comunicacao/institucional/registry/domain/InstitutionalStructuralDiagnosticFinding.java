package com.tcc.pjb.backend.core.comunicacao.institucional.registry.domain;

import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalRiskSeverity;
import java.util.List;

public record InstitutionalStructuralDiagnosticFinding(
        String code,
        InstitutionalRiskSeverity severity,
        boolean blocking,
        String targetType,
        String targetId,
        String message,
        List<String> evidences
) {
    public InstitutionalStructuralDiagnosticFinding {
        evidences = evidences == null ? List.of() : List.copyOf(evidences);
    }
}
