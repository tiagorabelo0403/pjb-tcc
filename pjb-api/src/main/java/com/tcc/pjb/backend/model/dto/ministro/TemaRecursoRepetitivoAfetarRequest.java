package com.tcc.pjb.backend.model.dto.ministro;

import jakarta.validation.constraints.NotBlank;

public record TemaRecursoRepetitivoAfetarRequest(
        String codigo,
        @NotBlank String ementa,
        String fundamentosResumo,
        String criterioAfetacao
) {
}
