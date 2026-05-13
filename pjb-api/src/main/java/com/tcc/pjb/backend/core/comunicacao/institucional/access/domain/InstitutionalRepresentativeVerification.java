package com.tcc.pjb.backend.core.comunicacao.institucional.access.domain;

import java.time.Instant;
import java.util.List;

public record InstitutionalRepresentativeVerification(
        String requestId,
        Long representativeUserId,
        String representativeName,
        String representativeRole,
        String authorityTitle,
        boolean representativeIdentityComplete,
        boolean representativeDocumentValidated,
        boolean institutionalDomainValidated,
        boolean certificateMaterialValidated,
        boolean trustChainValidated,
        boolean dualKeySatisfied,
        boolean homologationReady,
        List<String> findings,
        List<String> fundamentos,
        Instant checkedAt
) {
    public InstitutionalRepresentativeVerification {
        findings = findings == null ? List.of() : List.copyOf(findings);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
    }
}
