package com.tcc.pjb.backend.core.processo.prevencao.domain;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record ProcessoVinculacaoAnaliseItem(
        ProcessoVinculoTipo tipo,
        Long processoId,
        String numeroProcesso,
        String natureza,
        double score,
        boolean bloquearDistribuicao,
        boolean remeterPorPrevencao,
        String unidadeSugerida,
        List<String> chavesCompartilhadas,
        List<String> fundamentos,
        Instant distribuidoEm
) {
    public ProcessoVinculacaoAnaliseItem {
        tipo = Objects.requireNonNull(tipo, "tipo");
        numeroProcesso = Objects.toString(numeroProcesso, "").trim();
        natureza = Objects.toString(natureza, "").trim();
        score = Math.max(0d, Math.min(1d, score));
        unidadeSugerida = Objects.toString(unidadeSugerida, "").trim();
        chavesCompartilhadas = chavesCompartilhadas == null ? List.of() : List.copyOf(chavesCompartilhadas);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
        distribuidoEm = distribuidoEm == null ? Instant.EPOCH : distribuidoEm;
    }
}
