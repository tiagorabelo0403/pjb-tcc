package com.tcc.pjb.backend.core.plataforma.substituicao.domain;

import java.util.List;
import java.util.Objects;

public record PjbSubstituicaoFederativaNucleoDuroTribunal(
        String tribunalCodigo,
        String tribunalNome,
        String ramoJustica,
        String legadoPrincipal,
        String ondaAtual,
        int scoreGeral,
        int scoreComunicacaoSigilo,
        int scorePrevencaoRedistribuicao,
        int scoreFluxoRecursal,
        int scoreInfraestrutura,
        boolean prontoCutover,
        boolean prontoNucleoDuro,
        boolean prevencaoAtiva,
        boolean redistribuicaoAssistida,
        boolean fluxoRecursalPronto,
        int totalCompetencias,
        List<PjbSubstituicaoFederativaNucleoDuroCompetencia> competencias,
        List<String> bloqueadores,
        List<String> proximasAcoes,
        List<String> fundamentos
) {
    public PjbSubstituicaoFederativaNucleoDuroTribunal {
        tribunalCodigo = Objects.toString(tribunalCodigo, "").trim();
        tribunalNome = Objects.toString(tribunalNome, "").trim();
        ramoJustica = Objects.toString(ramoJustica, "").trim();
        legadoPrincipal = Objects.toString(legadoPrincipal, "").trim();
        ondaAtual = Objects.toString(ondaAtual, "").trim();
        scoreGeral = Math.max(0, Math.min(100, scoreGeral));
        scoreComunicacaoSigilo = Math.max(0, Math.min(100, scoreComunicacaoSigilo));
        scorePrevencaoRedistribuicao = Math.max(0, Math.min(100, scorePrevencaoRedistribuicao));
        scoreFluxoRecursal = Math.max(0, Math.min(100, scoreFluxoRecursal));
        scoreInfraestrutura = Math.max(0, Math.min(100, scoreInfraestrutura));
        totalCompetencias = Math.max(0, totalCompetencias);
        competencias = competencias == null ? List.of() : List.copyOf(competencias);
        bloqueadores = bloqueadores == null ? List.of() : List.copyOf(bloqueadores);
        proximasAcoes = proximasAcoes == null ? List.of() : List.copyOf(proximasAcoes);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
    }
}
