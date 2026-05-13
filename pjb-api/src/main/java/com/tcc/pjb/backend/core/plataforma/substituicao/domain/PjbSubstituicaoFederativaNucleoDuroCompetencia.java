package com.tcc.pjb.backend.core.plataforma.substituicao.domain;

import java.util.List;
import java.util.Objects;

public record PjbSubstituicaoFederativaNucleoDuroCompetencia(
        String ramoCodigo,
        String ramoDescricao,
        String ritoCodigo,
        int totalProcessos,
        int scoreComunicacaoSigilo,
        int scorePrevencao,
        int scoreRedistribuicao,
        int scoreFluxoRecursal,
        boolean prontoNucleoDuro,
        String unidadePreventa,
        String janelaAtual,
        List<String> guardrails,
        List<String> fundamentos,
        Long processoReferenciaId,
        String numeroReferencia
) {
    public PjbSubstituicaoFederativaNucleoDuroCompetencia {
        ramoCodigo = Objects.toString(ramoCodigo, "").trim();
        ramoDescricao = Objects.toString(ramoDescricao, "").trim();
        ritoCodigo = Objects.toString(ritoCodigo, "").trim();
        totalProcessos = Math.max(0, totalProcessos);
        scoreComunicacaoSigilo = Math.max(0, Math.min(100, scoreComunicacaoSigilo));
        scorePrevencao = Math.max(0, Math.min(100, scorePrevencao));
        scoreRedistribuicao = Math.max(0, Math.min(100, scoreRedistribuicao));
        scoreFluxoRecursal = Math.max(0, Math.min(100, scoreFluxoRecursal));
        unidadePreventa = Objects.toString(unidadePreventa, "").trim();
        janelaAtual = Objects.toString(janelaAtual, "").trim();
        guardrails = guardrails == null ? List.of() : List.copyOf(guardrails);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
        numeroReferencia = Objects.toString(numeroReferencia, "").trim();
    }
}
