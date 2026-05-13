package com.tcc.pjb.backend.model.dto.ui.accessibility;

import java.time.Instant;
import java.util.List;

public record UiAccessibilitySuggestionDto(
    UiAccessibilityPreset legacyPreset,
    long flagsMask,
    List<UiAccessibilityFlag> flags,
    int score,
    double probability,
    double confidence,
    List<String> reasonCodes,
    List<String> reasons,
    String suggestionHash,
    String token,
    Instant evaluatedAt
) {
}
