package com.tcc.pjb.backend.core.comunicacao.institucional.panel.domain;

import java.util.List;

public record InstitutionalPanelChart(
        String chartId,
        String title,
        String chartType,
        String accentColor,
        List<InstitutionalPanelChartPoint> points
) {
}
