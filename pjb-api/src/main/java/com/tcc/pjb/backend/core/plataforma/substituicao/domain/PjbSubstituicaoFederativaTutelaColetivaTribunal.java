package com.tcc.pjb.backend.core.plataforma.substituicao.domain;

import java.util.List;

public record PjbSubstituicaoFederativaTutelaColetivaTribunal(
        String tribunalCodigo,
        String tribunalNome,
        String ramoJustica,
        String legadoPrincipal,
        String ondaAtual,
        int scoreGeral,
        int scoreTutelaColetiva,
        int scoreDemandasEstruturais,
        int scoreExecucaoColetiva,
        int scoreCumprimentoMassa,
        boolean prontoMalhaPrecedentes,
        boolean malhaTutelaColetivaPronta,
        int totalCompetencias,
        List<PjbSubstituicaoFederativaTutelaColetivaCompetencia> competencias,
        List<String> bloqueadores,
        List<String> proximasAcoes,
        List<String> fundamentos
) {
}
