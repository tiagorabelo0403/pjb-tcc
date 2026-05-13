package com.tcc.pjb.backend.core.plataforma.substituicao.domain;

import java.util.List;

public record PjbSubstituicaoFederativaPrecedentesQualificadosTribunal(
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
        List<PjbSubstituicaoFederativaPrecedentesQualificadosCompetencia> competencias,
        List<String> bloqueadores,
        List<String> proximasAcoes,
        List<String> fundamentos
) {
    public PjbSubstituicaoFederativaPrecedentesQualificadosTribunal {
        tribunalCodigo = tribunalCodigo == null ? "NACIONAL" : tribunalCodigo;
        tribunalNome = tribunalNome == null ? tribunalCodigo : tribunalNome;
        ramoJustica = ramoJustica == null ? "ESTADUAL" : ramoJustica;
        legadoPrincipal = legadoPrincipal == null ? "PJB" : legadoPrincipal;
        ondaAtual = ondaAtual == null ? "shadow-mode-governado" : ondaAtual;
        competencias = competencias == null ? List.of() : List.copyOf(competencias);
        bloqueadores = bloqueadores == null ? List.of() : List.copyOf(bloqueadores);
        proximasAcoes = proximasAcoes == null ? List.of() : List.copyOf(proximasAcoes);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
        totalCompetencias = Math.max(totalCompetencias, competencias.size());
    }
}
