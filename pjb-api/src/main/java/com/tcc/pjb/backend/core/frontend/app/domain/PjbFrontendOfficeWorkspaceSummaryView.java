package com.tcc.pjb.backend.core.frontend.app.domain;

import java.util.List;

public record PjbFrontendOfficeWorkspaceSummaryView(
        Long equipeId,
        String officeName,
        Long founderUserId,
        String founderName,
        Long patronoUserId,
        String patronoNome,
        boolean currentUserFounder,
        boolean currentUserAffiliated,
        boolean currentWorkspaceSelected,
        String workspaceMode,
        long ownedOfficeCountForCurrentUser,
        long membershipCountForCurrentUser,
        long totalMembers,
        long activeMembers,
        long onlineMembers,
        boolean allBrazilianLawEnabled,
        boolean patronCertificateRequired,
        List<String> allowedRamos,
        List<String> blockers,
        List<String> hints,
        List<PjbFrontendOfficeTeamMemberView> members,
        List<PjbFrontendOfficeTeamMemberView> onlineTeamMembers
) {
}
