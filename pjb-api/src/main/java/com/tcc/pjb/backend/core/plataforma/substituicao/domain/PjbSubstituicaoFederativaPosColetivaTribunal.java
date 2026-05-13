package com.tcc.pjb.backend.core.plataforma.substituicao.domain;

import java.util.List;

public record PjbSubstituicaoFederativaPosColetivaTribunal(
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
        List<PjbSubstituicaoFederativaPosColetivaCompetencia> competencias,
        List<String> bloqueadores,
        List<String> proximasAcoes,
        List<String> fundamentos
) {
}
