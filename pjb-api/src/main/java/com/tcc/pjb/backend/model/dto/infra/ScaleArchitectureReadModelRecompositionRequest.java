package com.tcc.pjb.backend.model.dto.infra;

import jakarta.validation.constraints.NotBlank;

public record ScaleArchitectureReadModelRecompositionRequest(
        @NotBlank String domain,
        String tribunalCode,
        String ramoCode,
        String scopeKey,
        String requestedBy,
        String reason
) {
}
