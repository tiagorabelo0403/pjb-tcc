package com.tcc.pjb.backend.core.plataforma.substituicao.domain;

import java.util.List;

public record PjbSubstituicaoFederativaPrecedentesQualificadosCompetencia(
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
    public PjbSubstituicaoFederativaPrecedentesQualificadosCompetencia {
        competenciaCodigo = competenciaCodigo == null ? "COMPETENCIA" : competenciaCodigo;
        ramoCodigo = ramoCodigo == null ? "CIVIL" : ramoCodigo;
        ramoNome = ramoNome == null ? ramoCodigo : ramoNome;
        ritoCodigo = ritoCodigo == null ? "COMUM_ORDINARIO" : ritoCodigo;
        totalProcessos = Math.max(0, totalProcessos);
        guardrails = guardrails == null ? List.of() : List.copyOf(guardrails);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
    }
}
