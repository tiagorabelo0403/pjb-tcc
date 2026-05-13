package com.tcc.pjb.backend.core.plataforma.substituicao.domain;

import java.util.List;
import java.util.Objects;

public record PjbSubstituicaoFederativaWarRoomRamo(
        String ramoCodigo,
        String ramoDescricao,
        int score,
        boolean corteLiberado,
        boolean freezeAtivo,
        String janelaAtual,
        List<PjbSubstituicaoFederativaWarRoomRito> ritos,
        List<String> evidencias,
        List<String> acoes
) {
    public PjbSubstituicaoFederativaWarRoomRamo {
        ramoCodigo = Objects.toString(ramoCodigo, "").trim();
        ramoDescricao = Objects.toString(ramoDescricao, "").trim();
        score = Math.max(0, Math.min(100, score));
        janelaAtual = Objects.toString(janelaAtual, "").trim();
        ritos = ritos == null ? List.of() : List.copyOf(ritos);
        evidencias = evidencias == null ? List.of() : List.copyOf(evidencias);
        acoes = acoes == null ? List.of() : List.copyOf(acoes);
    }
}
