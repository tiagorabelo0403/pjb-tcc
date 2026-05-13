package com.tcc.pjb.backend.model.dto.ui.frontend;

public record FrontendOfficeProcessTransferDecisionRequest(
        Boolean acceptTerms,
        String idempotencyKey
) {
}
