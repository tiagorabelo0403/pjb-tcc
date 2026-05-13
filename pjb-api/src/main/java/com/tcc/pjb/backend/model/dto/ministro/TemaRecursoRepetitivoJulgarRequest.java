package com.tcc.pjb.backend.model.dto.ministro;

import jakarta.validation.constraints.NotBlank;

public record TemaRecursoRepetitivoJulgarRequest(
        String ementa,
        @NotBlank String teseFirmada,
        String fundamentosResumo
) {
}
