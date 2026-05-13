package com.tcc.pjb.backend.model.dto.ui.frontend;

public record FrontendOfficeAffiliationDecisionRequest(
        Boolean autoActivateOnLogin,
        Boolean allowPersonalOwnCases,
        String mode,
        Boolean acceptTerms,
        String idempotencyKey
) {
}
