package com.tcc.pjb.backend.model.dto.processual.substituicao.federativa.nucleoduro;

import java.util.List;

public record PjbSubstituicaoFederativaNucleoDuroCompetenciaResponse(
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
    public PjbSubstituicaoFederativaNucleoDuroCompetenciaResponse {
        guardrails = guardrails == null ? List.of() : List.copyOf(guardrails);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
    }
}
