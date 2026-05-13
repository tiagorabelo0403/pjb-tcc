package com.tcc.pjb.backend.core.comunicacao.institucional.registry.domain;

import java.time.Instant;
import java.util.List;

public record InstitutionalCoverageDelegationEntry(
        String delegationId,
        String sourceLotationId,
        Long sourceUserId,
        String sourceUserName,
        String targetLotationId,
        Long targetUserId,
        String targetUserName,
        String unitCode,
        String boxCode,
        String laneCode,
        String delegationKind,
        Instant activeFrom,
        Instant activeUntil,
        boolean active,
        boolean crossMunicipalitySupport,
        List<String> findings
) {
    public InstitutionalCoverageDelegationEntry {
        findings = findings == null ? List.of() : List.copyOf(findings);
    }
}
