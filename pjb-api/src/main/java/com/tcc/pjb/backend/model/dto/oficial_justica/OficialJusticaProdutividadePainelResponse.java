package com.tcc.pjb.backend.model.dto.oficial_justica;

import java.util.List;
import java.util.Map;

public record OficialJusticaProdutividadePainelResponse(
        Long oficialId,
        int diasJanela,
        int total,
        Map<String, Integer> porOutcome,
        Double taxaSucesso,
        Double intervaloMedioHoras,
        List<OficialJusticaProdutividadeItemResponse> itens
) {
}
