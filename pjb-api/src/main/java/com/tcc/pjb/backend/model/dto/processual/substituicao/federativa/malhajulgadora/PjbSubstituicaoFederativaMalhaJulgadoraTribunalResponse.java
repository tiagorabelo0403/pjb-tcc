package com.tcc.pjb.backend.model.dto.processual.substituicao.federativa.malhajulgadora;

import java.util.List;

public record PjbSubstituicaoFederativaMalhaJulgadoraTribunalResponse(
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
        List<PjbSubstituicaoFederativaMalhaJulgadoraUnidadeResponse> unidades,
        List<String> bloqueadores,
        List<String> proximasAcoes,
        List<String> fundamentos
) {
    public PjbSubstituicaoFederativaMalhaJulgadoraTribunalResponse {
        unidades = unidades == null ? List.of() : List.copyOf(unidades);
        bloqueadores = bloqueadores == null ? List.of() : List.copyOf(bloqueadores);
        proximasAcoes = proximasAcoes == null ? List.of() : List.copyOf(proximasAcoes);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
    }
}
