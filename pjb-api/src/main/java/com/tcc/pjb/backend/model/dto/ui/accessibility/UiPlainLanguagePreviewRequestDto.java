package com.tcc.pjb.backend.model.dto.ui.accessibility;

import jakarta.validation.constraints.NotBlank;

public record UiPlainLanguagePreviewRequestDto(
        @NotBlank String text
) {
}
