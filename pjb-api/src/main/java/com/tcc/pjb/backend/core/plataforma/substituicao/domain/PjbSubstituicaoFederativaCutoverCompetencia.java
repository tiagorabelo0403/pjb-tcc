package com.tcc.pjb.backend.core.plataforma.substituicao.domain;

import java.util.List;
import java.util.Objects;

public record PjbSubstituicaoFederativaCutoverCompetencia(
        String ramoCodigo,
        String ramoDescricao,
        String ritoCodigo,
        int totalProcessos,
        int scoreMaterial,
        int scoreComunicacao,
        int scoreSigilo,
        boolean corteLiberado,
        String janelaAtual,
        List<String> guardrails,
        List<String> proximasAcoes,
        Long processoReferenciaId,
        String numeroReferencia
) {
    public PjbSubstituicaoFederativaCutoverCompetencia {
        ramoCodigo = Objects.toString(ramoCodigo, "").trim();
        ramoDescricao = Objects.toString(ramoDescricao, "").trim();
        ritoCodigo = Objects.toString(ritoCodigo, "").trim();
        totalProcessos = Math.max(0, totalProcessos);
        scoreMaterial = Math.max(0, Math.min(100, scoreMaterial));
        scoreComunicacao = Math.max(0, Math.min(100, scoreComunicacao));
        scoreSigilo = Math.max(0, Math.min(100, scoreSigilo));
        janelaAtual = Objects.toString(janelaAtual, "").trim();
        guardrails = guardrails == null ? List.of() : List.copyOf(guardrails);
        proximasAcoes = proximasAcoes == null ? List.of() : List.copyOf(proximasAcoes);
        numeroReferencia = Objects.toString(numeroReferencia, "").trim();
    }
}
