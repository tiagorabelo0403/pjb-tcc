package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance;

import java.util.List;

public record InstitutionalRootAdminApprovalDecisionRequest(
        Long candidateUserId,
        String candidateUserName,
        String approvalSource,
        boolean approved,
        List<String> fundamentos
) {
}
