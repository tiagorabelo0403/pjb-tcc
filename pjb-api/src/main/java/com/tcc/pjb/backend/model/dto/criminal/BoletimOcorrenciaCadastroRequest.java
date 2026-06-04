package com.tcc.pjb.backend.model.dto.criminal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import java.time.Instant;

public record BoletimOcorrenciaCadastroRequest(
        @NotNull @Positive Long unidadeRegistroId,
        @NotBlank String naturezaFato,
        @NotBlank String resumoFatos,
        @NotBlank String localFato,
        @NotNull @PastOrPresent Instant ocorridoEm,
        @NotBlank String comunicanteResumo,
        String envolvidosResumo,
        @NotBlank String providenciasIniciais
) {
}
