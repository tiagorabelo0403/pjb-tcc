package com.tcc.pjb.backend.core.comunicacao.institucional.processual.domain;

import java.time.Instant;
import java.util.List;

public record InstitutionalProcessDiagnosticReport(
        boolean compliant,
        long totalFindings,
        long blockingFindings,
        List<InstitutionalProcessDiagnosticFinding> findings,
        List<String> fundamentos,
        Instant generatedAt
) {
    public InstitutionalProcessDiagnosticReport {
        findings = findings == null ? List.of() : List.copyOf(findings);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
    }
}
