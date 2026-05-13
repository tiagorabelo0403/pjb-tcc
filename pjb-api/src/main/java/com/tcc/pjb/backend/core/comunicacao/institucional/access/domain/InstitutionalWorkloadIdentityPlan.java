package com.tcc.pjb.backend.core.comunicacao.institucional.access.domain;

import java.time.Instant;
import java.util.List;

public record InstitutionalWorkloadIdentityPlan(
        String affiliationId,
        String orgaoSigla,
        String orgaoNome,
        String trustDomain,
        String namespace,
        boolean enabled,
        boolean mtlsRequired,
        boolean projectedServiceAccountTokenRequired,
        List<InstitutionalWorkloadIdentityBinding> workloads,
        List<String> findings,
        List<String> fundamentos,
        Instant generatedAt
) {
    public InstitutionalWorkloadIdentityPlan {
        workloads = workloads == null ? List.of() : List.copyOf(workloads);
        findings = findings == null ? List.of() : List.copyOf(findings);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
    }
}
