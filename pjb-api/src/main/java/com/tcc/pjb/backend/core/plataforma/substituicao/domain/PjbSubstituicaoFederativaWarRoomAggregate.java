package com.tcc.pjb.backend.core.plataforma.substituicao.domain;

import java.time.Instant;
import java.util.List;

public record PjbSubstituicaoFederativaWarRoomAggregate(
        int scoreGeral,
        boolean freezeNacionalAtivo,
        boolean prontoCorteControlado,
        int tribunaisComJanelaAberta,
        int tribunaisEmFreeze,
        List<String> bloqueadoresCriticos,
        List<PjbSubstituicaoFederativaWarRoomTribunal> tribunais,
        List<String> fundamentos,
        Instant geradoEm
) {
    public PjbSubstituicaoFederativaWarRoomAggregate {
        scoreGeral = Math.max(0, Math.min(100, scoreGeral));
        tribunaisComJanelaAberta = Math.max(0, tribunaisComJanelaAberta);
        tribunaisEmFreeze = Math.max(0, tribunaisEmFreeze);
        bloqueadoresCriticos = bloqueadoresCriticos == null ? List.of() : List.copyOf(bloqueadoresCriticos);
        tribunais = tribunais == null ? List.of() : List.copyOf(tribunais);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
        geradoEm = geradoEm == null ? Instant.now() : geradoEm;
    }
}
