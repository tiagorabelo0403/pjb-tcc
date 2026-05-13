package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance;

import java.time.Instant;
import java.util.List;

public record NationalCommunicationInstitutionalWorkloadIdentityPlanResponse(
        String affiliationId,
        String orgaoSigla,
        String orgaoNome,
        String trustDomain,
        String namespace,
        boolean enabled,
        boolean mtlsRequired,
        boolean projectedServiceAccountTokenRequired,
        List<NationalCommunicationInstitutionalWorkloadIdentityBindingResponse> workloads,
        List<String> findings,
        List<String> fundamentos,
        Instant generatedAt
) {
}
