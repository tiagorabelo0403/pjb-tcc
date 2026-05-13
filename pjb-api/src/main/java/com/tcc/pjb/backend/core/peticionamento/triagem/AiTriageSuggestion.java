package com.tcc.pjb.backend.core.peticionamento.triagem;

import java.util.List;

public record AiTriageSuggestion(
        String varaOuNucleoSugerido,
        String movimentoInicialSugerido,
        boolean sugerirConciliacao,
        String minutaDespachoDeSugestao,
        List<String> alertasIdentificados,
        double confianca,
        boolean requerRevisaoHumana
) {
    public AiTriageSuggestion {
        alertasIdentificados = alertasIdentificados == null ? List.of() : List.copyOf(alertasIdentificados);
        requerRevisaoHumana = true;
    }
}
