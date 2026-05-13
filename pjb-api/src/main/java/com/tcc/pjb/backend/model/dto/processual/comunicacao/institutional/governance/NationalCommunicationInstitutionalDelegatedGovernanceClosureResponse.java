package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance;

import java.time.Instant;
import java.util.List;

public record NationalCommunicationInstitutionalDelegatedGovernanceClosureResponse(
        String scopeFilter,
        List<String> perfisDiretosPermitidos,
        List<NationalCommunicationInstitutionalDelegatedScopeCoverageResponse> escoposDelegados,
        List<NationalCommunicationInstitutionalDelegatedGovernanceItemResponse> itens,
        List<String> fundamentos,
        Instant generatedAt
) {
}
