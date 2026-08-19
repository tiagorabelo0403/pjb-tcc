package com.tcc.pjb.backend.model.dto.oficial_justica;

public record HoraCertaExecucaoRequest(
        double latitude,
        double longitude,
        boolean destinatarioPresente,
        String observacoes
) {
}
