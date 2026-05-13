package com.tcc.pjb.backend.model.dto.processual.substituicao.federativa.poscoletiva;

import java.util.List;

public record PjbSubstituicaoFederativaPosColetivaCompetenciaResponse(
        String competenciaCodigo,
        String ramoCodigo,
        String ramoNome,
        String ritoCodigo,
        int totalProcessos,
        int scoreCoisaJulgadaColetiva,
        int scoreLiquidacaoColetiva,
        int scoreHabilitacaoIndividual,
        int scoreCumprimentoPulverizadoLotes,
        boolean malhaPosColetivaPronta,
        boolean coisaJulgadaColetivaAtiva,
        boolean liquidacaoColetivaAtiva,
        boolean habilitacaoIndividualAtiva,
        boolean cumprimentoPulverizadoLotesAtivo,
        String janelaAtual,
        List<String> guardrails,
        List<String> fundamentos,
        Long processoReferenciaId,
        String numeroReferencia
) {
}
