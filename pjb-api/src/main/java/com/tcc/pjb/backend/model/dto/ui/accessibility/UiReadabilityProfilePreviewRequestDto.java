package com.tcc.pjb.backend.model.dto.ui.accessibility;

import jakarta.validation.constraints.NotBlank;

public record UiReadabilityProfilePreviewRequestDto(
        @NotBlank String text,
        String audience,
        boolean lowVision,
        boolean screenReaderPrimary,
        boolean cognitiveLoadSensitive
) {
}
