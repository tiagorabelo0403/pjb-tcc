package com.tcc.pjb.backend.modules.prazos.api;

import java.time.LocalDate;

public record PrazoDiaForenseCommand(
        LocalDate data,
        String tribunalCodigo,
        String uf,
        String comarca,
        String ramo,
        String grau) {
}
