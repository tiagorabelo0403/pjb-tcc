package com.tcc.pjb.backend.core.frontend.app.domain;

import java.util.List;

public record PjbFrontendChartSeriesView(
        String key,
        String label,
        String chartType,
        String paletteKey,
        List<String> paletteHex,
        List<PjbFrontendChartPointView> points
) {
}
