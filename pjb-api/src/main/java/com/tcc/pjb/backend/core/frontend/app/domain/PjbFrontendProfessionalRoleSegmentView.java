package com.tcc.pjb.backend.core.frontend.app.domain;

import java.util.List;

public record PjbFrontendProfessionalRoleSegmentView(
        String key,
        String title,
        String subtitle,
        String accentHex,
        List<PjbFrontendAnalyticMetricCardView> metricCards,
        List<PjbFrontendChartSeriesView> charts,
        List<String> quickRoutes
) {
}
