package com.tcc.pjb.backend.core.frontend.app.domain;

import java.time.Instant;
import java.util.List;

public record PjbFrontendOfficeWorkspaceMainDashboardView(
        Instant generatedAt,
        String officeMode,
        Long activeEquipeId,
        String activeEquipeNome,
        PjbFrontendOfficeWorkspaceSummaryView officeSummary,
        PjbFrontendOfficeWorkspaceMainDashboardKpiView kpis,
        PjbFrontendOfficeWorkspaceLegalCockpitView legalCockpit,
        List<PjbFrontendOfficeTeamMemberView> onlineTeamMembers,
        List<PjbFrontendOfficeQueueItemView> pendingQueueItems,
        List<PjbFrontendOfficeProcessTransferView> pendingTransfers,
        List<PjbFrontendOfficeCriticalDeadlineView> criticalDeadlines,
        List<PjbFrontendOfficePendingPetitionView> pendingPetitions,
        List<String> quickRoutes,
        List<String> blockers,
        List<String> warnings
) {
}
