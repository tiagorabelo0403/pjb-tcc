package com.tcc.pjb.backend.core.comunicacao.institucional.registry.domain;

import java.time.Instant;
import java.util.List;

public record InstitutionalStructuralDiagnosticReport(
        String affiliationId,
        boolean compliant,
        long totalFindings,
        long blockingFindings,
        List<InstitutionalStructuralDiagnosticFinding> findings,
        List<String> fundamentos,
        Instant generatedAt
) {
    public InstitutionalStructuralDiagnosticReport {
        findings = findings == null ? List.of() : List.copyOf(findings);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
    }
}
