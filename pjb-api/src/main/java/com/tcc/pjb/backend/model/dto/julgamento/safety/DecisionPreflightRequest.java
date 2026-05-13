package com.tcc.pjb.backend.model.dto.julgamento.safety;

import jakarta.validation.constraints.NotBlank;

public record DecisionPreflightRequest(
        @NotBlank String actType,
        String primaryText,
        String reasoningText
) {
}
