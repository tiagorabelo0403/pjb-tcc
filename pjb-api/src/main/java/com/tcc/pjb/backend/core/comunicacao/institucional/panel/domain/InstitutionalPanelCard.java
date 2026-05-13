package com.tcc.pjb.backend.core.comunicacao.institucional.panel.domain;

public record InstitutionalPanelCard(
        String code,
        String title,
        long value,
        String subtitle,
        String accentColor,
        String trend,
        String icon,
        String navigationPath
) {

    public InstitutionalPanelCard {
        code = normalize(code);
        title = normalize(title);
        value = Math.max(0L, value);
        subtitle = normalize(subtitle);
        accentColor = normalize(accentColor);
        trend = normalize(trend);
        icon = normalize(icon);
        navigationPath = normalize(navigationPath);
    }

    public boolean isActionable() {
        return !navigationPath.isBlank();
    }

    public boolean hasTrend() {
        return !trend.isBlank();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
