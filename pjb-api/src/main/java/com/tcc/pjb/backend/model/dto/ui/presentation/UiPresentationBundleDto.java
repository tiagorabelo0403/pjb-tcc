package com.tcc.pjb.backend.model.dto.ui.presentation;

import java.time.Instant;

public record UiPresentationBundleDto(
    UiPresentationDto light,
    UiPresentationDto dark,
    Instant serverTime
) {
}
