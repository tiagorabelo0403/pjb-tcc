package com.tcc.pjb.backend.model.dto.processual.substituicao.nacional.cockpit;

public record PjbSubstituicaoNacionalCockpitResumoResponse(
        int totalTribunais,
        int totalExecucoes,
        int totalCutoversProntos,
        int totalRollbacksReversiveis,
        int totalHomologacoesBloqueadas,
        int totalMigracoesBloqueadas,
        int totalComunicacoesReprocessaveis
) {
}
