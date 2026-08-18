package com.tcc.pjb.backend.model.dto.juiz.produtividade;

import java.util.List;
import java.util.Map;

public record JuizProdutividadePainelResponse(
        Long magistradoId,
        int diasJanela,
        int total,
        Map<String, Integer> porTipo,
        Double intervaloMedioHoras,
        List<JuizProdutividadeAtoResponse> itens
) {
}
