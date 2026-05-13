package com.tcc.pjb.backend.model.dto.ui.accessibility;

import java.time.Instant;
import java.util.List;

public record UiAccessibilityPreferenceDto(
    UiAccessibilityPreset legacyPreset,
    long flagsMask,
    List<UiAccessibilityFlag> flags,
    boolean suppressSuggestions,
    Instant acceptedAt,
    Instant updatedAt,
    Instant lastEvaluatedAt,
    Instant nextEligibleAt
) {
}
