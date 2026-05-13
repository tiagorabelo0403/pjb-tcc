package com.tcc.pjb.backend.model.dto.ui.presentation;

import java.time.Instant;
import java.util.List;
import com.tcc.pjb.backend.model.dto.ui.UiTheme;
import com.tcc.pjb.backend.model.dto.ui.accessibility.UiAccessibilityFlag;
import com.tcc.pjb.backend.model.dto.ui.accessibility.UiAccessibilityPreset;

public record UiPresentationDto(
    UiTheme theme,
    UiPresentationVariant variant,
    UiAccessibilityPreset legacyAccessibilityPreset,
    long accessibilityFlagsMask,
    List<UiAccessibilityFlag> accessibilityFlags,
    boolean readingModeEnabled,
    UiReadingIntensity readingIntensity,
    List<UiCssTokenDto> tokens,
    String presentationHash,
    Instant generatedAt
) {
}
