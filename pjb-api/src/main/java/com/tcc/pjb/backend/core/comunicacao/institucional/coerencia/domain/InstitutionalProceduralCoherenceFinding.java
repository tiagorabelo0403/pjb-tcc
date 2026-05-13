package com.tcc.pjb.backend.core.comunicacao.institucional.coerencia.domain;

import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalRiskSeverity;
import java.util.List;
import java.util.Objects;
import com.tcc.pjb.backend.core.util.PayloadMaps;

public record InstitutionalProceduralCoherenceFinding(
        String code,
        InstitutionalRiskSeverity severity,
        boolean blocking,
        String message,
        List<String> evidences,
        List<String> fundamentos
) {
    public InstitutionalProceduralCoherenceFinding {
        Objects.requireNonNull(code);
        Objects.requireNonNull(severity);
        Objects.requireNonNull(message);
        evidences = PayloadMaps.copyDistinctStrings(evidences);
        fundamentos = PayloadMaps.copyDistinctStrings(fundamentos);
    }
}
