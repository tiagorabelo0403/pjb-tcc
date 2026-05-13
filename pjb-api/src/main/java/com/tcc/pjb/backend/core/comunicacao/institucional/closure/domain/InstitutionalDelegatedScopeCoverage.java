package com.tcc.pjb.backend.core.comunicacao.institucional.closure.domain;

import java.util.List;

public record InstitutionalDelegatedScopeCoverage(
        String organizationScope,
        String displayName,
        boolean delegatedInstitutionalEntry,
        boolean forumOrJudicialUnit,
        List<String> lanes,
        List<String> guardRails,
        List<String> fundamentos
) {
    public InstitutionalDelegatedScopeCoverage {
        lanes = lanes == null ? List.of() : List.copyOf(lanes);
        guardRails = guardRails == null ? List.of() : List.copyOf(guardRails);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
    }
}
