package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance;

import java.util.List;

public record NationalCommunicationInstitutionalTrustGovernanceDecisionRequest(
        String approvalKind,
        boolean approved,
        List<String> fundamentos
) {
}
