package com.tcc.pjb.backend.core.comunicacao.institucional.access.domain;

import java.time.Instant;
import java.util.List;

public record InstitutionalBindingApproval(
        Long userId,
        String userName,
        String affiliationId,
        String nominationId,
        String unidadeCodigo,
        String caixaCodigo,
        boolean affiliationActive,
        boolean nominationActive,
        boolean dualAdministrationSatisfied,
        boolean recertificationDue,
        boolean capacityBound,
        boolean homologated,
        boolean approved,
        List<String> findings,
        List<String> fundamentos,
        Instant checkedAt
) {
    public InstitutionalBindingApproval {
        findings = findings == null ? List.of() : List.copyOf(findings);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
    }
}
