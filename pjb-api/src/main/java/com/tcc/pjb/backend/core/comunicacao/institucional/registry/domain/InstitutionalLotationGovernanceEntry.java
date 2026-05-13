package com.tcc.pjb.backend.core.comunicacao.institucional.registry.domain;

import java.time.Instant;
import java.util.List;

public record InstitutionalLotationGovernanceEntry(
        String lotationId,
        String nominationId,
        Long userId,
        String userName,
        String unitCode,
        String boxCode,
        String laneCode,
        String nominationRole,
        String operationalFunction,
        String trustFloor,
        boolean active,
        Instant activeFrom,
        Instant activeUntil,
        List<String> findings
) {
    public InstitutionalLotationGovernanceEntry {
        findings = findings == null ? List.of() : List.copyOf(findings);
    }
}
