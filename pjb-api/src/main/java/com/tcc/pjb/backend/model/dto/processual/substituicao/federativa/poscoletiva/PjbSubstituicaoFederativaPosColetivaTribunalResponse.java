package com.tcc.pjb.backend.model.dto.processual.substituicao.federativa.poscoletiva;

import java.util.List;

public record PjbSubstituicaoFederativaPosColetivaTribunalResponse(
        String tribunalCodigo,
        String tribunalNome,
        String ramoJustica,
        String legadoPrincipal,
        String ondaAtual,
        int scoreGeral,
        int scoreCoisaJulgadaColetiva,
        int scoreLiquidacaoColetiva,
        int scoreHabilitacaoIndividual,
        int scoreCumprimentoPulverizadoLotes,
        boolean prontoTutelaColetiva,
        boolean malhaPosColetivaPronta,
        int totalCompetencias,
        List<PjbSubstituicaoFederativaPosColetivaCompetenciaResponse> competencias,
        List<String> bloqueadores,
        List<String> proximasAcoes,
        List<String> fundamentos
) {
}
