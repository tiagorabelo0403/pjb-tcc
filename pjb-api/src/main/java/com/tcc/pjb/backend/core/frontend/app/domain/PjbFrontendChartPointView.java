package com.tcc.pjb.backend.core.frontend.app.domain;

public record PjbFrontendChartPointView(
        String key,
        String label,
        long value,
        String accentTone,
        String accentHex
) {
}
