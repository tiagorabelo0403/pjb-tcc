package com.tcc.pjb.backend.core.processo.conexao.domain;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record ProcessoConexaoAggregate(
        Long processoIdRaiz,
        String numeroProcessoRaiz,
        boolean haConexao,
        int totalConexos,
        List<ProcessoConexaoItem> itens,
        List<String> fundamentos,
        Instant geradoEm
) {
    public ProcessoConexaoAggregate {
        numeroProcessoRaiz = Objects.toString(numeroProcessoRaiz, "").trim();
        totalConexos = Math.max(0, totalConexos);
        itens = itens == null ? List.of() : List.copyOf(itens);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
        geradoEm = geradoEm == null ? Instant.now() : geradoEm;
    }
}
