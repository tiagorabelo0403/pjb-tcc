package com.tcc.pjb.backend.model.dto.cidadao;

import java.util.List;

public record CidadaoPastaProcessosResponse(
        int page,
        int size,
        long totalElements,
        int totalPages,
        List<CidadaoProcessoCardDto> processos,
        String uiLegendUrl,
        AreaLinks links
) {
}
