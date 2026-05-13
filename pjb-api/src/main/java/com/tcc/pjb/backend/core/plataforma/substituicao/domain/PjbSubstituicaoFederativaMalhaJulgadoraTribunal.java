package com.tcc.pjb.backend.core.plataforma.substituicao.domain;

import java.util.List;
import java.util.Objects;

public record PjbSubstituicaoFederativaMalhaJulgadoraTribunal(
        String tribunalCodigo,
        String tribunalNome,
        String ramoJustica,
        String legadoPrincipal,
        String ondaAtual,
        int scoreGeral,
        int scoreIncidentes,
        int scoreColegiados,
        int scoreUnidadesJulgadoras,
        boolean prontoNucleoDuro,
        boolean malhaJulgadoraPronta,
        int totalUnidades,
        List<PjbSubstituicaoFederativaMalhaJulgadoraUnidade> unidades,
        List<String> bloqueadores,
        List<String> proximasAcoes,
        List<String> fundamentos
) {
    public PjbSubstituicaoFederativaMalhaJulgadoraTribunal {
        tribunalCodigo = Objects.toString(tribunalCodigo, "").trim();
        tribunalNome = Objects.toString(tribunalNome, "").trim();
        ramoJustica = Objects.toString(ramoJustica, "").trim();
        legadoPrincipal = Objects.toString(legadoPrincipal, "").trim();
        ondaAtual = Objects.toString(ondaAtual, "").trim();
        scoreGeral = Math.max(0, Math.min(100, scoreGeral));
        scoreIncidentes = Math.max(0, Math.min(100, scoreIncidentes));
        scoreColegiados = Math.max(0, Math.min(100, scoreColegiados));
        scoreUnidadesJulgadoras = Math.max(0, Math.min(100, scoreUnidadesJulgadoras));
        totalUnidades = Math.max(0, totalUnidades);
        unidades = unidades == null ? List.of() : List.copyOf(unidades);
        bloqueadores = bloqueadores == null ? List.of() : List.copyOf(bloqueadores);
        proximasAcoes = proximasAcoes == null ? List.of() : List.copyOf(proximasAcoes);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
    }
}
