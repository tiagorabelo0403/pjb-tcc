package com.tcc.pjb.backend.core.frontend.app.domain;

import java.util.List;

public record PjbFrontendOfficeModeView(
        String mode,
        Long activeEquipeId,
        String activeEquipeNome,
        Long activeSeniorUserId,
        String activeSeniorNome,
        boolean officeProcessesVisibleByDefault,
        boolean canOpenPersonalOwnCases,
        boolean autoActivateOnLogin,
        boolean personalBlockedByOfficePolicy,
        boolean requiresOfficeSelection,
        boolean detached,
        List<PjbFrontendOfficeMembershipView> memberships,
        List<String> switchHints,
        List<String> effectiveAllowedRamos,
        boolean canViewAllRamos,
        Integer currentTrustScore,
        String currentTrustLevel,
        Integer requiredMinTrustForAuto,
        boolean patronCertificateRequired,
        Long effectiveSignerUserId,
        String effectiveSignerNome
) {
}
