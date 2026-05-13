package com.tcc.pjb.backend.core.comunicacao.institucional.registry.domain;

import java.time.Instant;
import java.util.List;

public record InstitutionalUnitGovernanceSnapshot(
        String snapshotId,
        String affiliationId,
        String orgaoSigla,
        String orgaoNome,
        String organizationScope,
        String status,
        int totalUnits,
        int totalBoxes,
        int totalLotacoes,
        List<InstitutionalManagedUnitEntry> units,
        List<InstitutionalLotationGovernanceEntry> lotacoes,
        List<String> findings,
        List<String> fundamentos,
        Instant generatedAt
) {
    public InstitutionalUnitGovernanceSnapshot {
        units = units == null ? List.of() : List.copyOf(units);
        lotacoes = lotacoes == null ? List.of() : List.copyOf(lotacoes);
        findings = findings == null ? List.of() : List.copyOf(findings);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
    }
}
