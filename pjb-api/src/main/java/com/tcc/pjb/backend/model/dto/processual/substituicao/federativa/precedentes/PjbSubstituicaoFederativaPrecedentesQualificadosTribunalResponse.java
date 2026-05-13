package com.tcc.pjb.backend.model.dto.processual.substituicao.federativa.precedentes;

import java.util.List;

public record PjbSubstituicaoFederativaPrecedentesQualificadosTribunalResponse(
        String tribunalCodigo,
        String tribunalNome,
        String ramoJustica,
        String legadoPrincipal,
        String ondaAtual,
        int scoreGeral,
        int scoreIncidentesMassa,
        int scoreTemasAfetados,
        int scoreSobrestamento,
        int scorePrecedentesVinculantes,
        boolean prontoMalhaJulgadora,
        boolean malhaPrecedentesPronta,
        int totalCompetencias,
        List<PjbSubstituicaoFederativaPrecedentesQualificadosCompetenciaResponse> competencias,
        List<String> bloqueadores,
        List<String> proximasAcoes,
        List<String> fundamentos
) {
    public PjbSubstituicaoFederativaPrecedentesQualificadosTribunalResponse {
        competencias = competencias == null ? List.of() : List.copyOf(competencias);
        bloqueadores = bloqueadores == null ? List.of() : List.copyOf(bloqueadores);
        proximasAcoes = proximasAcoes == null ? List.of() : List.copyOf(proximasAcoes);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
    }
}
