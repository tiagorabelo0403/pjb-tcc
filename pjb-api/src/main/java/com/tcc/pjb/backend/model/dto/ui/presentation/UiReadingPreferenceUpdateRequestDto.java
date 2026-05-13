package com.tcc.pjb.backend.model.dto.ui.presentation;

public record UiReadingPreferenceUpdateRequestDto(
    boolean readingModeEnabled,
    UiReadingIntensity intensity
) {
}
