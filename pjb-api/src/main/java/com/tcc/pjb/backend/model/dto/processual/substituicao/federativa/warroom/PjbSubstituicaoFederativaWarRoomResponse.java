package com.tcc.pjb.backend.model.dto.processual.substituicao.federativa.warroom;

import java.time.Instant;
import java.util.List;

public record PjbSubstituicaoFederativaWarRoomResponse(
        int scoreGeral,
        boolean freezeNacionalAtivo,
        boolean prontoCorteControlado,
        int tribunaisComJanelaAberta,
        int tribunaisEmFreeze,
        List<String> bloqueadoresCriticos,
        List<PjbSubstituicaoFederativaWarRoomTribunalResponse> tribunais,
        List<String> fundamentos,
        Instant geradoEm
) {
    public PjbSubstituicaoFederativaWarRoomResponse {
        bloqueadoresCriticos = bloqueadoresCriticos == null ? List.of() : List.copyOf(bloqueadoresCriticos);
        tribunais = tribunais == null ? List.of() : List.copyOf(tribunais);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
        geradoEm = geradoEm == null ? Instant.now() : geradoEm;
    }
}
