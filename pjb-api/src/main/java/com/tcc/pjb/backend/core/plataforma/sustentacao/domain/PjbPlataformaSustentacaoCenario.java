package com.tcc.pjb.backend.core.plataforma.sustentacao.domain;

import java.util.List;

public record PjbPlataformaSustentacaoCenario(
        String codigo,
        String titulo,
        String tribunalCodigo,
        String ramo,
        String rito,
        int score,
        boolean apto,
        List<String> alertas,
        List<String> fundamentos
) {
    public PjbPlataformaSustentacaoCenario {
        alertas = alertas == null ? List.of() : List.copyOf(alertas);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
    }
}
