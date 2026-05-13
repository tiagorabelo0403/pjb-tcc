package com.tcc.pjb.backend.model.dto.ui.frontend;

import jakarta.validation.constraints.NotBlank;

public record FrontendOfficeModeUpdateRequest(
        Long equipeId,
        @NotBlank String mode,
        Boolean autoActivateOnLogin,
        Boolean allowPersonalOwnCases
) {
}
