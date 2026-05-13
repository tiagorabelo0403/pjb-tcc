package com.tcc.pjb.backend.model.dto.processual.substituicao.federativa.malhajulgadora;

import java.util.List;

public record PjbSubstituicaoFederativaMalhaJulgadoraUnidadeResponse(
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
    public PjbSubstituicaoFederativaMalhaJulgadoraUnidadeResponse {
        guardrails = guardrails == null ? List.of() : List.copyOf(guardrails);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
    }
}
