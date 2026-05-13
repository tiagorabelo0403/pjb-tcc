package com.tcc.pjb.backend.core.comunicacao.institucional.access.domain;

import java.util.List;

public record InstitutionalWorkloadIdentityBinding(
        String workloadCode,
        String displayName,
        String spiffeId,
        String serviceAccount,
        String namespace,
        String audience,
        boolean mtlsRequired,
        boolean projectedTokenRequired,
        List<String> egressPolicies,
        List<String> fundamentos
) {
    public InstitutionalWorkloadIdentityBinding {
        egressPolicies = egressPolicies == null ? List.of() : List.copyOf(egressPolicies);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
    }
}
