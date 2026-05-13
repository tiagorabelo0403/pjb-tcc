package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance;

import java.util.List;

public record NationalCommunicationInstitutionalWorkloadIdentityBindingResponse(
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
}
