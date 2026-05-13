package com.tcc.pjb.backend.model.dto.pericia;

import java.time.LocalDate;
import java.time.LocalTime;

public record PeritoDisponibilidadeResponse(
        Long id,
        Long peritoId,
        String peritoNome,
        String especialidadeCodigo,
        String comarca,
        LocalDate dataInicio,
        LocalDate dataFim,
        LocalTime horaInicio,
        LocalTime horaFim,
        boolean disponivel,
        String motivoIndisponibilidade
) {
}
