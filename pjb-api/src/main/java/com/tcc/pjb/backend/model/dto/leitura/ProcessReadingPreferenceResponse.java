package com.tcc.pjb.backend.model.dto.leitura;

public record ProcessReadingPreferenceResponse(
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
