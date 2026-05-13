package com.tcc.pjb.backend.core.plataforma.substituicao.domain;

import java.util.List;
import java.util.Objects;

public record PjbSubstituicaoFederativaCutoverTribunal(
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
        List<PjbSubstituicaoFederativaCutoverCompetencia> competencias,
        List<String> bloqueadores,
        List<String> fundamentos
) {
    public PjbSubstituicaoFederativaCutoverTribunal {
        tribunalCodigo = Objects.toString(tribunalCodigo, "").trim();
        tribunalNome = Objects.toString(tribunalNome, "").trim();
        ramoJustica = Objects.toString(ramoJustica, "").trim();
        legadoPrincipal = Objects.toString(legadoPrincipal, "").trim();
        ondaAtual = Objects.toString(ondaAtual, "").trim();
        scoreGeral = Math.max(0, Math.min(100, scoreGeral));
        scoreMaterial = Math.max(0, Math.min(100, scoreMaterial));
        scoreComunicacao = Math.max(0, Math.min(100, scoreComunicacao));
        scoreSigilo = Math.max(0, Math.min(100, scoreSigilo));
        scoreGovernanca = Math.max(0, Math.min(100, scoreGovernanca));
        janelaAtual = Objects.toString(janelaAtual, "").trim();
        totalCompetencias = Math.max(0, totalCompetencias);
        competencias = competencias == null ? List.of() : List.copyOf(competencias);
        bloqueadores = bloqueadores == null ? List.of() : List.copyOf(bloqueadores);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
    }
}
