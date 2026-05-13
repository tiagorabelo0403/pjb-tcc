package com.tcc.pjb.backend.model.dto.ui.frontend;

public record FrontendOfficeAffiliationFinalApprovalRequest(
        Boolean confirmActivation,
        String idempotencyKey,
        String justification
) {
}
