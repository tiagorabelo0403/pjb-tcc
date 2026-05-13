package com.tcc.pjb.backend.model.dto.processual.substituicao.federativa.precedentes;

import java.util.List;

public record PjbSubstituicaoFederativaPrecedentesQualificadosCompetenciaResponse(
        String competenciaCodigo,
        String ramoCodigo,
        String ramoNome,
        String ritoCodigo,
        int totalProcessos,
        int scoreIncidentesMassa,
        int scoreAfetacao,
        int scoreSobrestamento,
        int scorePrecedentesVinculantes,
        boolean malhaPrecedentesPronta,
        boolean incidenteMassaAtivo,
        boolean afetacaoAtiva,
        boolean sobrestamentoAtivo,
        boolean precedenteVinculanteAtivo,
        boolean painelDemandasRepetitivasAtivo,
        String janelaAtual,
        List<String> guardrails,
        List<String> fundamentos,
        Long processoReferenciaId,
        String numeroReferencia
) {
    public PjbSubstituicaoFederativaPrecedentesQualificadosCompetenciaResponse {
        guardrails = guardrails == null ? List.of() : List.copyOf(guardrails);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
    }
}
