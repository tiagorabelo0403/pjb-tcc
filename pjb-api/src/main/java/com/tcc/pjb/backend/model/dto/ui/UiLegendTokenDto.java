package com.tcc.pjb.backend.model.dto.ui;

public record UiLegendTokenDto(
    UiToken token,
    String colorHex,
    String onColorHex,
    String icon,
    String pattern,
    String label,
    String description
) {
}
