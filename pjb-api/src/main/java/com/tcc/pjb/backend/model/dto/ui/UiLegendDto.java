package com.tcc.pjb.backend.model.dto.ui;

import java.time.Instant;
import java.util.List;

public record UiLegendDto(
    String persona,
    UiTheme theme,
    int version,
    Instant generatedAt,
    List<UiLegendTokenDto> tokens
) {
}
