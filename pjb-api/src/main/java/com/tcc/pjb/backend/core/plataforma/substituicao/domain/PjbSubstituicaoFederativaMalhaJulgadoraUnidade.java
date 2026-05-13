package com.tcc.pjb.backend.core.plataforma.substituicao.domain;

import java.util.List;
import java.util.Objects;

public record PjbSubstituicaoFederativaMalhaJulgadoraUnidade(
        String unidadeCodigo,
        String unidadeNome,
        String ramoCodigo,
        String ritoCodigo,
        int totalProcessos,
        int scoreIncidentes,
        int scoreColegiado,
        int scorePrevencaoRedistribuicao,
        boolean malhaJulgadoraPronta,
        boolean possuiIncidenteAtivo,
        boolean possuiColegiadoAtivo,
        String janelaAtual,
        List<String> guardrails,
        List<String> fundamentos,
        Long processoReferenciaId,
        String numeroReferencia
) {
    public PjbSubstituicaoFederativaMalhaJulgadoraUnidade {
        unidadeCodigo = Objects.toString(unidadeCodigo, "").trim();
        unidadeNome = Objects.toString(unidadeNome, "").trim();
        ramoCodigo = Objects.toString(ramoCodigo, "").trim();
        ritoCodigo = Objects.toString(ritoCodigo, "").trim();
        totalProcessos = Math.max(0, totalProcessos);
        scoreIncidentes = Math.max(0, Math.min(100, scoreIncidentes));
        scoreColegiado = Math.max(0, Math.min(100, scoreColegiado));
        scorePrevencaoRedistribuicao = Math.max(0, Math.min(100, scorePrevencaoRedistribuicao));
        janelaAtual = Objects.toString(janelaAtual, "").trim();
        guardrails = guardrails == null ? List.of() : List.copyOf(guardrails);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
        numeroReferencia = Objects.toString(numeroReferencia, "").trim();
    }
}
