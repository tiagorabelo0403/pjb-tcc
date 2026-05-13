package com.tcc.pjb.backend.core.processo.prevencao.domain;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record ProcessoPrevencaoItem(
        Long processoId,
        String numeroProcesso,
        String natureza,
        double score,
        boolean bloquearDistribuicao,
        boolean remeterPorPrevencao,
        String unidadeSugerida,
        List<String> fundamentos,
        Instant distribuidoEm
) {
    public ProcessoPrevencaoItem {
        numeroProcesso = Objects.toString(numeroProcesso, "").trim();
        natureza = Objects.toString(natureza, "").trim();
        score = Math.max(0d, Math.min(1d, score));
        unidadeSugerida = Objects.toString(unidadeSugerida, "").trim();
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
        distribuidoEm = distribuidoEm == null ? Instant.EPOCH : distribuidoEm;
    }
}
