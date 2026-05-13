package com.tcc.pjb.backend.model.dto.processual.substituicao.federativa.warroom;

import java.util.List;

public record PjbSubstituicaoFederativaWarRoomRitoResponse(
        String ritoCodigo,
        int score,
        String readiness,
        String resilience,
        String observability,
        String janelaAtual,
        boolean corteLiberado,
        boolean freezeAtivo,
        List<String> alertas,
        List<String> acoesImediatas,
        Long processoReferenciaId,
        String numeroReferencia
) {
    public PjbSubstituicaoFederativaWarRoomRitoResponse {
        alertas = alertas == null ? List.of() : List.copyOf(alertas);
        acoesImediatas = acoesImediatas == null ? List.of() : List.copyOf(acoesImediatas);
    }
}
