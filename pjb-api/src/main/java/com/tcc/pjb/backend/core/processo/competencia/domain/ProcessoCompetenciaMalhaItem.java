package com.tcc.pjb.backend.core.processo.competencia.domain;

import java.util.List;
import java.util.Objects;

public record ProcessoCompetenciaMalhaItem(
        String codigo,
        String eixo,
        String acao,
        String destinoTribunal,
        String destinoUnidade,
        boolean bloqueante,
        double score,
        List<String> fundamentos
) {
    public ProcessoCompetenciaMalhaItem {
        codigo = Objects.toString(codigo, "").trim();
        eixo = Objects.toString(eixo, "").trim();
        acao = Objects.toString(acao, "").trim();
        destinoTribunal = Objects.toString(destinoTribunal, "").trim();
        destinoUnidade = Objects.toString(destinoUnidade, "").trim();
        score = Math.max(0d, Math.min(1d, score));
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
    }
}
