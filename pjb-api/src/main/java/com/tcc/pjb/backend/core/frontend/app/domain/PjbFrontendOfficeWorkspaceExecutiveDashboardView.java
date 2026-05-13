package com.tcc.pjb.backend.core.frontend.app.domain;

import java.time.Instant;
import java.util.List;

public record PjbFrontendOfficeWorkspaceExecutiveDashboardView(
        Instant generatedAt,
        String officeMode,
        Long activeEquipeId,
        String activeEquipeNome,
        PjbFrontendVisualThemeView visualTheme,
        PjbFrontendOfficeWorkspaceSummaryView officeSummary,
        PjbFrontendOfficeWorkspaceMainDashboardKpiView kpis,
        PjbFrontendOfficeWorkspaceMainDashboardView operationalSnapshot,
        PjbFrontendOfficeWorkspaceLegalCockpitView legalCockpit,
        List<PjbFrontendAnalyticMetricCardView> headlineCards,
        List<PjbFrontendChartSeriesView> charts,
        List<PjbFrontendAvatarCardView> teamGallery,
        List<PjbFrontendAvatarCardView> profileGallery,
        List<PjbFrontendWorkspaceBoardColumnView> board,
        List<String> linkedModules,
        List<String> quickRoutes,
        List<String> blockers,
        List<String> warnings
) {
}
