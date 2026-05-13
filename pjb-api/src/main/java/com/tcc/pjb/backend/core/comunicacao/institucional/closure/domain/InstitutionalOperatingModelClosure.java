package com.tcc.pjb.backend.core.comunicacao.institucional.closure.domain;

import java.time.Instant;
import java.util.List;

public record InstitutionalOperatingModelClosure(
        String affiliationId,
        String orgaoSigla,
        String orgaoNome,
        String destinatarioKind,
        String organizationScope,
        String blueprintCode,
        String entryMode,
        boolean institutionManagedRoles,
        boolean personalRootIdentityRequired,
        boolean magistratesEnterThroughForumAndPersonalAccess,
        String coverageMode,
        InstitutionalOperatingCoverageRoute coverageRoute,
        List<InstitutionalOperatingAdministrativeSeat> administrativeSeats,
        List<InstitutionalOperatingRoleBand> roleBands,
        List<String> findings,
        List<String> fundamentos,
        Instant generatedAt
) {
    public InstitutionalOperatingModelClosure {
        administrativeSeats = administrativeSeats == null ? List.of() : List.copyOf(administrativeSeats);
        roleBands = roleBands == null ? List.of() : List.copyOf(roleBands);
        findings = findings == null ? List.of() : List.copyOf(findings);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
    }
}
