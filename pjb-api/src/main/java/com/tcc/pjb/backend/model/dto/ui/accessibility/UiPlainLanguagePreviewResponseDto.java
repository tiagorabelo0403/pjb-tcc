package com.tcc.pjb.backend.model.dto.ui.accessibility;

import java.util.Map;

public record UiPlainLanguagePreviewResponseDto(
        String originalText,
        String simplifiedText,
        Map<String, Integer> metrics
) {
}
