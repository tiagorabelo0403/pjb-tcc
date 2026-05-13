package com.tcc.pjb.backend.model.dto.profile.operational;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SecretariaVisibilidadePainelPessoalRequest(
        @NotNull Boolean visivel,
        @NotBlank String fundamento,
        @Min(1) @Max(3650) Integer diasValidade
) {
}
