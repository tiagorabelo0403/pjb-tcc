package com.tcc.pjb.backend.model.dto.processual.prazo;

import java.time.LocalDate;

public record DiaForenseResponse(
        LocalDate data,
        boolean diaUtil,
        String motivo,
        String tipoEntrada) {
}
