package com.tcc.pjb.backend.model.dto.processual.substituicao.federativa.cutover;

import java.util.List;

public record PjbSubstituicaoFederativaCutoverTribunalResponse(
        String tribunalCodigo,
        String tribunalNome,
        String ramoJustica,
        String legadoPrincipal,
        String ondaAtual,
        int scoreGeral,
        int scoreMaterial,
        int scoreComunicacao,
        int scoreSigilo,
        int scoreGovernanca,
        boolean corteLiberado,
        boolean freezeAtivo,
        String janelaAtual,
        int totalCompetencias,
        List<PjbSubstituicaoFederativaCutoverCompetenciaResponse> competencias,
        List<String> bloqueadores,
        List<String> fundamentos
) {
    public PjbSubstituicaoFederativaCutoverTribunalResponse {
        competencias = competencias == null ? List.of() : List.copyOf(competencias);
        bloqueadores = bloqueadores == null ? List.of() : List.copyOf(bloqueadores);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
    }
}
