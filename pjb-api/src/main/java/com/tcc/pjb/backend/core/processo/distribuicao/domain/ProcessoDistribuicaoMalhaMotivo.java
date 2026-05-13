package com.tcc.pjb.backend.core.processo.distribuicao.domain;

import java.util.List;
import java.util.Objects;

public record ProcessoDistribuicaoMalhaMotivo(
        String codigo,
        String dominio,
        String severidade,
        boolean bloqueante,
        String resumo,
        String referencia,
        List<String> fundamentos
) {
    public ProcessoDistribuicaoMalhaMotivo {
        codigo = Objects.toString(codigo, "").trim();
        dominio = Objects.toString(dominio, "").trim();
        severidade = Objects.toString(severidade, "").trim();
        resumo = Objects.toString(resumo, "").trim();
        referencia = Objects.toString(referencia, "").trim();
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
    }
}
