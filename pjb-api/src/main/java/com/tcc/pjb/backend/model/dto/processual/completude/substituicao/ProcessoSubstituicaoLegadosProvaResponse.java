package com.tcc.pjb.backend.model.dto.processual.completude.substituicao;

import java.util.List;

public record ProcessoSubstituicaoLegadosProvaResponse(
        String codigo,
        String titulo,
        String status,
        int score,
        boolean concluida,
        List<String> fundamentos,
        List<String> bloqueios
) {
    public ProcessoSubstituicaoLegadosProvaResponse {
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
        bloqueios = bloqueios == null ? List.of() : List.copyOf(bloqueios);
    }
}
