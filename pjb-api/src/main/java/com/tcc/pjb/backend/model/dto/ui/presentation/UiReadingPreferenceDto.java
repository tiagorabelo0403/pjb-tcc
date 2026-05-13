package com.tcc.pjb.backend.model.dto.ui.presentation;

import java.time.Instant;

public record UiReadingPreferenceDto(
    boolean readingModeEnabled,
    UiReadingIntensity intensity,
    Instant updatedAt
) {
}
