package com.tcc.pjb.backend.model.dto.juiz.produtividade;

import java.time.Instant;

public record JuizProdutividadeAtoResponse(
        Long movimentacaoId,
        Long processoId,
        String tipo,
        Instant dataMovimentacao
) {
}
