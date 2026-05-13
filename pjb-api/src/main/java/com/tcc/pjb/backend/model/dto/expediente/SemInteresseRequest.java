package com.tcc.pjb.backend.model.dto.expediente;

import jakarta.validation.constraints.NotNull;

public record SemInteresseRequest(
        @NotNull Long expedienteId,
        String justificativa
) {
}
