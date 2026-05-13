package com.tcc.pjb.backend.model.dto.ui.accessibility;

import java.util.List;
import java.util.Map;

public record UiReadabilityProfilePreviewResponseDto(
        UiAccessibilityPreset preset,
        List<UiAccessibilityFlag> recommendedFlags,
        Map<String, Integer> metrics,
        List<String> recommendations
) {
}
