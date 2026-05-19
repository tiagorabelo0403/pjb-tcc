package com.tcc.pjb.backend.modules.prazos.api;

import java.time.LocalDate;

public record PrazoDiaForenseResult(
        LocalDate data,
        boolean diaUtil,
        String motivo,
        String tipoEntrada,
        boolean conferenciaManualRecomendada) {

    public PrazoDiaForenseResult comConferenciaManual(boolean valor) {
        return new PrazoDiaForenseResult(data, diaUtil, motivo, tipoEntrada, valor);
    }
}
