package com.tcc.pjb.backend.model.dto.institutional.produtividade;

import java.util.List;
import java.util.Map;

public record InstitutionalProdutividadePainelResponse(
        Long atorId,
        int diasJanela,
        int total,
        Map<String, Integer> porTipo,
        Double intervaloMedioHoras,
        List<InstitutionalProdutividadeItemResponse> itens
) {
}
