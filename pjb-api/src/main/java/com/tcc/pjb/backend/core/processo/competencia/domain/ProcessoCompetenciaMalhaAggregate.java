package com.tcc.pjb.backend.core.processo.competencia.domain;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record ProcessoCompetenciaMalhaAggregate(
        Long processoId,
        String numeroProcesso,
        String ramo,
        String eixoCompetencia,
        String tribunalAtual,
        String unidadeAtual,
        String tribunalSugerido,
        String unidadeSugerida,
        String acaoPrimaria,
        boolean exigeRemessa,
        boolean exigeRedistribuicao,
        boolean travaAtosIncompativeis,
        List<ProcessoCompetenciaMalhaItem> itens,
        List<String> fundamentos,
        Instant geradoEm
) {
    public ProcessoCompetenciaMalhaAggregate {
        numeroProcesso = Objects.toString(numeroProcesso, "").trim();
        ramo = Objects.toString(ramo, "").trim();
        eixoCompetencia = Objects.toString(eixoCompetencia, "").trim();
        tribunalAtual = Objects.toString(tribunalAtual, "").trim();
        unidadeAtual = Objects.toString(unidadeAtual, "").trim();
        tribunalSugerido = Objects.toString(tribunalSugerido, "").trim();
        unidadeSugerida = Objects.toString(unidadeSugerida, "").trim();
        acaoPrimaria = Objects.toString(acaoPrimaria, "").trim();
        itens = itens == null ? List.of() : List.copyOf(itens);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
        geradoEm = geradoEm == null ? Instant.now() : geradoEm;
    }
}
