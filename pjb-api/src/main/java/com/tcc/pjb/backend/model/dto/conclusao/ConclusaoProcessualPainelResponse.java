package com.tcc.pjb.backend.model.dto.conclusao;

import java.util.List;

public record ConclusaoProcessualPainelResponse(
        Long magistradoId,
        int total,
        List<ConclusaoProcessualResponse> itens
) {
}
