package com.tcc.pjb.backend.service.document.reading;

public record ProcessReadingPresetProfile(
        boolean readingModeEnabled,
        String intensity,
        String presetCode,
        String resolvedTheme,
        int fontScalePercent,
        double lineHeight,
        double paragraphGapRem,
        double letterSpacingEm,
        int maxWidthCh,
        int chunkPageSize,
        String focusBandMode,
        String privacyVeilMode,
        String keyboardBiasMode,
        String chronologyMode,
        String citationMode,
        String operationalOverlayMode,
        String searchAssistMode,
        String anchorMode
) {
}
