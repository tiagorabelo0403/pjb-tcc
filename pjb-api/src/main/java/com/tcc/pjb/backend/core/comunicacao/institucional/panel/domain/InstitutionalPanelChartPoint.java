package com.tcc.pjb.backend.core.comunicacao.institucional.panel.domain;

public record InstitutionalPanelChartPoint(
        String label,
        double value,
        String accentColor,
        String tooltip
) {
}
