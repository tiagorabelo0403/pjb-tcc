package com.tcc.pjb.backend.core.frontend.app.domain;

import java.util.List;

public record PjbFrontendVisualThemeView(
        String key,
        String label,
        String primaryHex,
        String secondaryHex,
        String accentHex,
        String supportHex,
        String neutralHex,
        String backgroundHex,
        String surfaceHex,
        String surfaceAltHex,
        String gradientStartHex,
        String gradientEndHex,
        List<String> chartPaletteHex
) {
}
