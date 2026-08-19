package com.tcc.pjb.backend.model.dto.curadoria;

import jakarta.validation.constraints.NotBlank;

public record NomearCuradorRequest(
        @NotBlank String nomeCurador,
        String oabOuFuncional
) {
}
