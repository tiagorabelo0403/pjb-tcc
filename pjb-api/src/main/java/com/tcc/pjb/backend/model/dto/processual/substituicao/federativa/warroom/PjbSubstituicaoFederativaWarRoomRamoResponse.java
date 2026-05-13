package com.tcc.pjb.backend.model.dto.processual.substituicao.federativa.warroom;

import java.util.List;

public record PjbSubstituicaoFederativaWarRoomRamoResponse(
        String ramoCodigo,
        String ramoDescricao,
        int score,
        boolean corteLiberado,
        boolean freezeAtivo,
        String janelaAtual,
        List<PjbSubstituicaoFederativaWarRoomRitoResponse> ritos,
        List<String> evidencias,
        List<String> acoes
) {
    public PjbSubstituicaoFederativaWarRoomRamoResponse {
        ritos = ritos == null ? List.of() : List.copyOf(ritos);
        evidencias = evidencias == null ? List.of() : List.copyOf(evidencias);
        acoes = acoes == null ? List.of() : List.copyOf(acoes);
    }
}
