package com.tcc.pjb.backend.core.frontend.app.domain;

import java.util.List;

public record PjbFrontendOwnedOfficeView(
        Long equipeId,
        String officeName,
        Long founderUserId,
        String founderName,
        boolean policyEnabled,
        boolean allBrazilianLawEnabled,
        List<String> allowedRamos,
        String defaultMode,
        boolean autoActivateOnLogin,
        boolean patronCertificateRequired,
        long totalMembers,
        long activeMembers,
        long onlineMembers,
        boolean currentWorkspaceSelected
) {
}
