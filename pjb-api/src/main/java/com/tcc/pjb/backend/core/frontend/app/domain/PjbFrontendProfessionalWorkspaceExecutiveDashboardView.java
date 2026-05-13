package com.tcc.pjb.backend.core.frontend.app.domain;

import java.time.Instant;
import java.util.List;

public record PjbFrontendProfessionalWorkspaceExecutiveDashboardView(
        Instant generatedAt,
        String actorClass,
        String panelMode,
        String displayRole,
        String territorialAnchor,
        PjbFrontendVisualThemeView visualTheme,
        List<PjbFrontendAnalyticMetricCardView> headlineCards,
        List<PjbFrontendChartSeriesView> charts,
        List<PjbFrontendAvatarCardView> profileGallery,
        List<PjbFrontendWorkspaceBoardColumnView> board,
        List<PjbFrontendProfessionalProcessSpotlightView> spotlightProcesses,
        List<String> linkedModules,
        List<String> quickRoutes,
        List<String> warnings
) {
}
