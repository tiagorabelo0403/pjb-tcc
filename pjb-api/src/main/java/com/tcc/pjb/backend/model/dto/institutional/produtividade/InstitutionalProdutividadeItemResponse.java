package com.tcc.pjb.backend.model.dto.institutional.produtividade;

import java.time.Instant;

public record InstitutionalProdutividadeItemResponse(
        Long movimentacaoId,
        Long processoId,
        String tipo,
        Instant dataMovimentacao
) {
}
