package com.tcc.pjb.backend.core.comunicacao.institucional.access.domain;

import java.time.Instant;
import java.util.List;

public record InstitutionalStrongSignaturePolicy(
        String affiliationId,
        String nominationId,
        Long userId,
        String userName,
        String laneCode,
        boolean signOrSubmitCapability,
        boolean managedCredentialActive,
        boolean govBrRequired,
        boolean govBrSatisfied,
        boolean govBrPrataOuroRequired,
        boolean govBrPrataOuroSatisfied,
        boolean qualifiedCertificateRequired,
        boolean qualifiedCertificateSatisfied,
        boolean trustedNetworkOrRemoteAuthorizationRequired,
        boolean trustedNetworkOrRemoteAuthorizationSatisfied,
        boolean mfaRequired,
        boolean mfaSatisfied,
        boolean rootAdministrationApprovalRequired,
        boolean rootAdministrationApprovalSatisfied,
        boolean allowed,
        List<String> findings,
        List<String> fundamentos,
        Instant generatedAt
) {
    public InstitutionalStrongSignaturePolicy {
        findings = findings == null ? List.of() : List.copyOf(findings);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
    }
}
