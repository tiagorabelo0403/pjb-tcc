package com.tcc.pjb.backend.core.frontend.app.domain;

import java.time.Instant;
import java.util.List;

public record PjbFrontendProfessionalRoleExecutiveDashboardView(
        Instant generatedAt,
        String actorClass,
        String dashboardKind,
        String title,
        String organizationalLens,
        PjbFrontendVisualThemeView visualTheme,
        List<PjbFrontendAnalyticMetricCardView> headlineCards,
        List<PjbFrontendProfessionalRoleSegmentView> segments,
        List<PjbFrontendAvatarCardView> profileGallery,
        List<PjbFrontendWorkspaceBoardColumnView> board,
        List<PjbFrontendProfessionalProcessSpotlightView> spotlightProcesses,
        List<String> linkedModules,
        List<String> quickRoutes,
        List<String> warnings
) {
}
