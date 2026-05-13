package com.tcc.pjb.backend.model.dto.pericia;

import java.time.LocalDate;
import java.time.LocalTime;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PeritoDisponibilidadeRequest(
        @NotBlank String especialidadeCodigo,
        String comarca,
        @NotNull LocalDate dataInicio,
        @NotNull LocalDate dataFim,
        LocalTime horaInicio,
        LocalTime horaFim,
        boolean disponivel,
        String motivoIndisponibilidade
) {
}
