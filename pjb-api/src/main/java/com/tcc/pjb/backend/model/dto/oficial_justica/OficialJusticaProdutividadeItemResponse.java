package com.tcc.pjb.backend.model.dto.oficial_justica;

import java.time.Instant;

public record OficialJusticaProdutividadeItemResponse(
        Long encerramentoId,
        Long processoId,
        String processoNumero,
        String outcome,
        Instant createdAt
) {
}
