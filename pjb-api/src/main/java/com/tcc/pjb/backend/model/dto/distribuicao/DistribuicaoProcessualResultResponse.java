package com.tcc.pjb.backend.model.dto.distribuicao;

public record DistribuicaoProcessualResultResponse(
        Long workItemId,
        String status,
        String filaDistribuicao,
        String inboxKey,
        String varaDestino,
        String comarcaDestino,
        String trilhoCompetencia
) {
}
