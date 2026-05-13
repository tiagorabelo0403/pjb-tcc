package com.tcc.pjb.backend.core.comunicacao.institucional.access.domain;

import java.time.Instant;
import java.util.List;

public record InstitutionalContextActivationDecision(
        Long userId,
        String userName,
        String identityCode,
        String affiliationId,
        String nominationId,
        String unidadeCodigo,
        String caixaCodigo,
        boolean personalIdentityAuthenticated,
        boolean institutionalBindingValid,
        boolean operationalContextActive,
        boolean requiresStepUp,
        boolean requiresManualApproval,
        boolean blocked,
        boolean allowed,
        List<String> findings,
        List<String> fundamentos,
        Instant checkedAt
) {
    public InstitutionalContextActivationDecision {
        findings = findings == null ? List.of() : List.copyOf(findings);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
    }
}
