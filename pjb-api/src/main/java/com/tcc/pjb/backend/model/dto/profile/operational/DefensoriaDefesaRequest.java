package com.tcc.pjb.backend.model.dto.profile.operational;

import jakarta.validation.constraints.NotBlank;

public record DefensoriaDefesaRequest(
        @NotBlank String defesa,
        @NotBlank String fundamentacao
) {}
