package com.tcc.pjb.backend.model.dto.processual.completude.substituicao;

import java.util.List;

public record ProcessoSubstituicaoLegadosSistemaResponse(
        String sistema,
        String status,
        int scoreAderencia,
        String conclusao,
        List<String> pendencias
) {
    public ProcessoSubstituicaoLegadosSistemaResponse {
        pendencias = pendencias == null ? List.of() : List.copyOf(pendencias);
    }
}
