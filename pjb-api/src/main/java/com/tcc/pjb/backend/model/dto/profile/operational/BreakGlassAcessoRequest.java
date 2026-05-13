package com.tcc.pjb.backend.model.dto.profile.operational;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record BreakGlassAcessoRequest(
        @NotBlank String escopoAcesso,
        @NotBlank String justificativa,
        String fundamentoAprovacao,
        @NotNull Boolean stepUpSatisfeito,
        @Min(1) @Max(168) Integer horasValidade
) {
}
