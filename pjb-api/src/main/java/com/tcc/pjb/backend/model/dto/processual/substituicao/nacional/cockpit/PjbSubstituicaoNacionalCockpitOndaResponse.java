package com.tcc.pjb.backend.model.dto.processual.substituicao.nacional.cockpit;

public record PjbSubstituicaoNacionalCockpitOndaResponse(
        String ondaCodigo,
        int totalExecucoes,
        int totalTribunais,
        int gateMedio,
        int cutoversProntos,
        int rollbacksReversiveis
) {
}
