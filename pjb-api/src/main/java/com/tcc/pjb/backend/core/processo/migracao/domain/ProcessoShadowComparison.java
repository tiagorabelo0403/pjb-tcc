package com.tcc.pjb.backend.core.processo.migracao.domain;

import java.util.List;
import java.util.Objects;

public record ProcessoShadowComparison(
        String codigo,
        String titulo,
        String criticidade,
        boolean blocking,
        String expected,
        String actual,
        List<String> fundamentos
) {
    public ProcessoShadowComparison {
        Objects.requireNonNull(codigo);
        Objects.requireNonNull(titulo);
        criticidade = criticidade == null ? "CONTROLADA" : criticidade;
        expected = expected == null ? "NAO_INFORMADO" : expected;
        actual = actual == null ? "NAO_INFORMADO" : actual;
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
    }
}
