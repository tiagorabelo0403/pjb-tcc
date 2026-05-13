package com.tcc.pjb.backend.core.frontend.app.domain;

public record PjbFrontendAnalyticMetricCardView(
        String key,
        String label,
        String value,
        String secondaryValue,
        String accentTone,
        String accentHex,
        String surfaceHex,
        String contentHex,
        String route
) {
}
