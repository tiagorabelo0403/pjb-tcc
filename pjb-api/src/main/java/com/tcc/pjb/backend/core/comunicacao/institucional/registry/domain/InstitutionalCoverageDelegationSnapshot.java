package com.tcc.pjb.backend.core.comunicacao.institucional.registry.domain;

import java.time.Instant;
import java.util.List;

public record InstitutionalCoverageDelegationSnapshot(
        String snapshotId,
        String affiliationId,
        String status,
        int totalDelegations,
        int activeDelegations,
        List<InstitutionalCoverageDelegationEntry> delegations,
        List<String> findings,
        List<String> fundamentos,
        Instant generatedAt
) {
    public InstitutionalCoverageDelegationSnapshot {
        delegations = delegations == null ? List.of() : List.copyOf(delegations);
        findings = findings == null ? List.of() : List.copyOf(findings);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
    }
}
