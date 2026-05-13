package com.tcc.pjb.backend.model.dto.processual.substituicao.federativa.tutelacoletiva;

import java.util.List;

public record PjbSubstituicaoFederativaTutelaColetivaTribunalResponse(
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
        List<PjbSubstituicaoFederativaTutelaColetivaCompetenciaResponse> competencias,
        List<String> bloqueadores,
        List<String> proximasAcoes,
        List<String> fundamentos
) {
}
