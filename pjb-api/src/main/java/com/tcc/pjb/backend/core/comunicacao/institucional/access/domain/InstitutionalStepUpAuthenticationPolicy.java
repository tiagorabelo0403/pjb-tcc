package com.tcc.pjb.backend.core.comunicacao.institucional.access.domain;

import java.time.Instant;
import java.util.List;

public record InstitutionalStepUpAuthenticationPolicy(
        Long userId,
        String userName,
        String affiliationId,
        String nominationId,
        String sensitiveAct,
        boolean requiresMfa,
        boolean requiresQualifiedCertificate,
        boolean requiresInstitutionalNetwork,
        boolean acceptsRemoteCertificateAuthorization,
        boolean requiresManualApproval,
        boolean blocked,
        List<String> findings,
        List<String> fundamentos,
        Instant checkedAt
) {
    public InstitutionalStepUpAuthenticationPolicy {
        findings = findings == null ? List.of() : List.copyOf(findings);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
    }
}
