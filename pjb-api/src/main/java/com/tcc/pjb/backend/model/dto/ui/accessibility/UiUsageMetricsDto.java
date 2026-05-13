package com.tcc.pjb.backend.model.dto.ui.accessibility;

import java.util.Map;

public record UiUsageMetricsDto(
    Boolean prefersReducedMotion,
    Boolean forcedColors,
    Boolean screenReaderHint,
    Integer zoomEventsLast30d,
    Integer highContrastTogglesLast30d,
    Integer keyboardNavigationRate,
    Integer mouseNavigationRate,
    Integer fontScalePercent,
    Map<String, String> headersEvidence
) {
}
