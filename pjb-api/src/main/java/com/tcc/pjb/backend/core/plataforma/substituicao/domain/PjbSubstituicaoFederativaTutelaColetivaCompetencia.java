package com.tcc.pjb.backend.core.plataforma.substituicao.domain;

import java.util.List;

public record PjbSubstituicaoFederativaTutelaColetivaCompetencia(
        String competenciaCodigo,
        String ramoCodigo,
        String ramoNome,
        String ritoCodigo,
        int totalProcessos,
        int scoreTutelaColetiva,
        int scoreDemandasEstruturais,
        int scoreExecucaoColetiva,
        int scoreCumprimentoMassa,
        boolean malhaTutelaColetivaPronta,
        boolean tutelaColetivaAtiva,
        boolean demandaEstruturalAtiva,
        boolean execucaoColetivaAtiva,
        boolean cumprimentoMassaAtivo,
        boolean roteamentoColetivoAtivo,
        String janelaAtual,
        List<String> guardrails,
        List<String> fundamentos,
        Long processoReferenciaId,
        String numeroReferencia
) {
}
