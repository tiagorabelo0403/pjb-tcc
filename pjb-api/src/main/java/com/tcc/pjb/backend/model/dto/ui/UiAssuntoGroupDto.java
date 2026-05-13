package com.tcc.pjb.backend.model.dto.ui;


public record UiAssuntoGroupDto(
    String id,
    String colorHex,
    String onColorHex,
    String icon,
    String pattern,
    String label,
    String description
) {
}
