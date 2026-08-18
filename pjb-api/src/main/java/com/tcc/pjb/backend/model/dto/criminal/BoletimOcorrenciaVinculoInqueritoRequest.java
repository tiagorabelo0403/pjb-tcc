package com.tcc.pjb.backend.model.dto.criminal;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record BoletimOcorrenciaVinculoInqueritoRequest(
        @NotNull @Positive Long inqueritoId
) {
}
