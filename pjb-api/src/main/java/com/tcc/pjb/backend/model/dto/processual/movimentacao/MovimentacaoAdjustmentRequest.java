package com.tcc.pjb.backend.model.dto.processual.movimentacao;

public record MovimentacaoAdjustmentRequest(
        String modo,
        String motivo,
        String descricaoSubstitutiva
) {
}
