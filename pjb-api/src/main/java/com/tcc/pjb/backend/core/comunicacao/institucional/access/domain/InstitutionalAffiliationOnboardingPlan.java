package com.tcc.pjb.backend.core.comunicacao.institucional.access.domain;

import java.time.Instant;
import java.util.List;

public record InstitutionalAffiliationOnboardingPlan(
        String affiliationId,
        String orgaoSigla,
        String orgaoNome,
        String unidadeCodigo,
        String organizationScope,
        String blueprintCode,
        String coverageMode,
        String responsibleUnitCode,
        String responsibleUnitName,
        boolean selfServiceInstitutionManagedUsers,
        boolean govBrRootIdentityRequired,
        boolean signerDualEvidenceRequired,
        boolean dualAdministrationApprovalRequired,
        List<InstitutionalOnboardingStep> steps,
        List<InstitutionalAuthenticationLanePolicy> lanePolicies,
        List<String> findings,
        List<String> fundamentos,
        Instant generatedAt
) {
    public InstitutionalAffiliationOnboardingPlan {
        steps = steps == null ? List.of() : List.copyOf(steps);
        lanePolicies = lanePolicies == null ? List.of() : List.copyOf(lanePolicies);
        findings = findings == null ? List.of() : List.copyOf(findings);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
    }
}
