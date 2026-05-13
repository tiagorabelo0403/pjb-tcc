package com.tcc.pjb.backend.model.dto.processual.substituicao.federativa.cutover;

import java.util.List;

public record PjbSubstituicaoFederativaCutoverCompetenciaResponse(
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
    public PjbSubstituicaoFederativaCutoverCompetenciaResponse {
        guardrails = guardrails == null ? List.of() : List.copyOf(guardrails);
        proximasAcoes = proximasAcoes == null ? List.of() : List.copyOf(proximasAcoes);
    }
}
