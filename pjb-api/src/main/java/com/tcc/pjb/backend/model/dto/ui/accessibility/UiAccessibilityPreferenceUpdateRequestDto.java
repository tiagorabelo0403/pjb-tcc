package com.tcc.pjb.backend.model.dto.ui.accessibility;

import java.util.List;

public record UiAccessibilityPreferenceUpdateRequestDto(
    UiAccessibilityPreset legacyPreset,
    Long flagsMask,
    List<UiAccessibilityFlag> flags,
    boolean suppressSuggestions,
    String decision,
    String token
) {
}
