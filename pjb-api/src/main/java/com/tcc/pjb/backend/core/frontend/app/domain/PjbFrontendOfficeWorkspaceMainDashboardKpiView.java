package com.tcc.pjb.backend.core.frontend.app.domain;

public record PjbFrontendOfficeWorkspaceMainDashboardKpiView(
        long totalMembers,
        long onlineMembers,
        long carteiraTotalProcessos,
        long pendingPatronQueueItems,
        long pendingTransfers,
        long criticalDeadlines,
        long pendingPetitions,
        long upcomingHearings,
        long unreadIntimacoes,
        long recursosVencendo
) {
}
