package com.tcc.pjb.backend.core.comunicacao.institucional.coerencia.domain;

import java.time.Instant;
import java.util.List;
import com.tcc.pjb.backend.core.util.PayloadMaps;

public record InstitutionalProceduralCoherenceDiagnosticReport(
        boolean compliant,
        int totalFindings,
        long blockingFindings,
        List<InstitutionalProceduralCoherenceFinding> findings,
        List<String> fundamentos,
        Instant generatedAt
) {
    public InstitutionalProceduralCoherenceDiagnosticReport {
        findings = PayloadMaps.copyListDistinct(findings);
        fundamentos = PayloadMaps.copyDistinctStrings(fundamentos);
        generatedAt = generatedAt == null ? Instant.now() : generatedAt;
    }
}
