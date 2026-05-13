package com.tcc.pjb.backend.core.plataforma.substituicao.domain;

import java.util.List;
import java.util.Objects;

public record PjbSubstituicaoFederativaWarRoomRito(
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
    public PjbSubstituicaoFederativaWarRoomRito {
        ritoCodigo = Objects.toString(ritoCodigo, "").trim();
        score = Math.max(0, Math.min(100, score));
        readiness = Objects.toString(readiness, "").trim();
        resilience = Objects.toString(resilience, "").trim();
        observability = Objects.toString(observability, "").trim();
        janelaAtual = Objects.toString(janelaAtual, "").trim();
        alertas = alertas == null ? List.of() : List.copyOf(alertas);
        acoesImediatas = acoesImediatas == null ? List.of() : List.copyOf(acoesImediatas);
        numeroReferencia = Objects.toString(numeroReferencia, "").trim();
    }
}
