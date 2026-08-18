package com.tcc.pjb.backend.modules.prazos.domain;

import java.time.LocalDate;

public record PrazoProcessualParametros(
        LocalDate data,
        String tipoPrazo,
        String ramo,
        String grau,
        String tribunalCodigo,
        String uf,
        String comarca,
        Integer diasOverride) {
}
