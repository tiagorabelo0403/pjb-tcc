package com.tcc.pjb.backend.core.processo.unificado.domain;

import java.util.List;
import java.util.Objects;

public record ProcessoMalhaNacionalRisco(
        String codigo,
        String dominio,
        String severidade,
        boolean bloqueante,
        String titulo,
        String detalhe,
        String acaoSugerida,
        List<String> fundamentos
) {
    public ProcessoMalhaNacionalRisco {
        codigo = Objects.toString(codigo, "").trim();
        dominio = Objects.toString(dominio, "GERAL").trim();
        severidade = Objects.toString(severidade, "ATENCAO").trim();
        titulo = Objects.toString(titulo, "").trim();
        detalhe = Objects.toString(detalhe, "").trim();
        acaoSugerida = Objects.toString(acaoSugerida, "").trim();
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
    }
}
