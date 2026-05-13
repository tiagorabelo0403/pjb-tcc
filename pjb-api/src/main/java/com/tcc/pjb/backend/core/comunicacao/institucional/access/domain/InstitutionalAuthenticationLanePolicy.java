package com.tcc.pjb.backend.core.comunicacao.institucional.access.domain;

import java.util.List;

public record InstitutionalAuthenticationLanePolicy(
        String laneCode,
        String laneKind,
        String nominationRole,
        String funcaoOperacional,
        String processProfile,
        String displayName,
        boolean requiresGovBrRootIdentity,
        String minimumGovBrLevel,
        boolean allowsInstitutionManagedLogin,
        boolean requiresInstitutionManagedLogin,
        boolean requiresMfaAtEntry,
        boolean requiresQualifiedCertificateForEntry,
        boolean requiresQualifiedCertificateForSensitiveActs,
        boolean requiresInstitutionalNetwork,
        boolean allowsRemoteAuthorizedCertificate,
        boolean signsOrSubmitsSensitiveActs,
        List<String> capacidades,
        List<String> fundamentos
) {
    public InstitutionalAuthenticationLanePolicy {
        capacidades = capacidades == null ? List.of() : List.copyOf(capacidades);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
    }
}
