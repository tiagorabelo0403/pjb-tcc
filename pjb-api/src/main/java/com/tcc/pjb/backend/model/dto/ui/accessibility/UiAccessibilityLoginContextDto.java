package com.tcc.pjb.backend.model.dto.ui.accessibility;

import java.time.Instant;
import java.util.List;
import com.tcc.pjb.backend.model.dto.ui.presentation.UiPresentationBundleDto;

public record UiAccessibilityLoginContextDto(
    UiAccessibilityPreset currentLegacyPreset,
    long currentFlagsMask,
    List<UiAccessibilityFlag> currentFlags,
    boolean suppressSuggestions,
    Instant nextEligibleAt,
    UiAccessibilitySuggestionDto suggestion,
    UiPresentationBundleDto presentation,
    Instant serverTime
) {
}
