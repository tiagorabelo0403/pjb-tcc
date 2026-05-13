package com.tcc.pjb.backend.core.processo.conexao.domain;

import java.util.List;
import java.util.Objects;

public record ProcessoConexaoItem(
        Long processoId,
        String numeroProcesso,
        String natureza,
        double score,
        List<String> chavesCompartilhadas,
        List<String> fundamentos
) {
    public ProcessoConexaoItem {
        numeroProcesso = Objects.toString(numeroProcesso, "").trim();
        natureza = Objects.toString(natureza, "").trim();
        score = Math.max(0d, Math.min(1d, score));
        chavesCompartilhadas = chavesCompartilhadas == null ? List.of() : List.copyOf(chavesCompartilhadas);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
    }
}
