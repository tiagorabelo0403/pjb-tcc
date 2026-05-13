package com.tcc.pjb.backend.model.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RitoFeedbackRequest(
        @NotNull Long processoId,
        @NotBlank String ritoChosen,
        String notes,
        Boolean applyOverride
) {
}
