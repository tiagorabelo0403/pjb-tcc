package com.tcc.pjb.backend.model.dto.pericia;

import java.time.LocalDate;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PeritoSorteioRequest(
        @NotBlank String especialidadeCodigo,
        String comarca,
        @NotNull LocalDate data,
        Long processoId
) {
}
