package com.tcc.pjb.backend.core.frontend.app.domain;

public record PjbFrontendOfficeAffiliationDecisionResultView(
        PjbFrontendOfficeAffiliationInviteView invite,
        PjbFrontendOfficeModeView officeMode,
        boolean activated,
        boolean awaitingFinalApproval
) {
}
