package com.tcc.pjb.backend.core.processo.prevencao.domain;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record ProcessoPrevencaoAggregate(
        Long processoIdRaiz,
        String numeroProcessoRaiz,
        boolean haPrevencao,
        String processoPrevento,
        String unidadeSugerida,
        List<ProcessoPrevencaoItem> itens,
        List<String> fundamentos,
        Instant geradoEm
) {
    public ProcessoPrevencaoAggregate {
        numeroProcessoRaiz = Objects.toString(numeroProcessoRaiz, "").trim();
        processoPrevento = Objects.toString(processoPrevento, "").trim();
        unidadeSugerida = Objects.toString(unidadeSugerida, "").trim();
        itens = itens == null ? List.of() : List.copyOf(itens);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
        geradoEm = geradoEm == null ? Instant.now() : geradoEm;
    }


    public boolean temPrevencao() {
        return haPrevencao;
    }

    public String unidadePreventa() {
        return unidadeSugerida;
    }
}
