package com.tcc.pjb.backend.model.dto.processual.substituicao.federativa.nucleoduro;

import java.util.List;

public record PjbSubstituicaoFederativaNucleoDuroTribunalResponse(
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
        List<PjbSubstituicaoFederativaNucleoDuroCompetenciaResponse> competencias,
        List<String> bloqueadores,
        List<String> proximasAcoes,
        List<String> fundamentos
) {
    public PjbSubstituicaoFederativaNucleoDuroTribunalResponse {
        competencias = competencias == null ? List.of() : List.copyOf(competencias);
        bloqueadores = bloqueadores == null ? List.of() : List.copyOf(bloqueadores);
        proximasAcoes = proximasAcoes == null ? List.of() : List.copyOf(proximasAcoes);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
    }
}
