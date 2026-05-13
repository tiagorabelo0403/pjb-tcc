package com.tcc.pjb.backend.core.comunicacao.institucional.access.domain;

import java.time.Instant;
import java.util.List;

public record InstitutionalAuthenticationPolicyClosure(
        String affiliationId,
        String orgaoSigla,
        String orgaoNome,
        String unidadeCodigo,
        String organizationScope,
        String blueprintCode,
        boolean personalRootIdentityRequired,
        boolean managedInstitutionalLoginSupported,
        boolean managedInstitutionalLoginRequiresGovBrBinding,
        boolean dualEvidenceRequiredForSensitiveActs,
        boolean qualifiedCertificateRequiredForSigners,
        boolean trustedNetworkOrRemoteAuthorizationRequiredForCertificates,
        List<InstitutionalAuthenticationLanePolicy> lanePolicies,
        List<String> findings,
        List<String> fundamentos,
        Instant generatedAt
) {
    public InstitutionalAuthenticationPolicyClosure {
        lanePolicies = lanePolicies == null ? List.of() : List.copyOf(lanePolicies);
        findings = findings == null ? List.of() : List.copyOf(findings);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
    }
}
