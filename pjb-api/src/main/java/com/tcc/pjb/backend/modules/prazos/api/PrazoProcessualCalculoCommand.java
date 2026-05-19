package com.tcc.pjb.backend.modules.prazos.api;

import java.time.LocalDate;

public record PrazoProcessualCalculoCommand(
        LocalDate dataInicio,
        String tipoPrazo,
        String ramo,
        String grau,
        String tribunalCodigo,
        String uf,
        String comarca,
        Integer diasOverride) {
}
