package com.tcc.pjb.backend.model.dto.processual.substituicao.legados;

import java.util.List;

public record PjbSubstituicaoLegadosSistemaResponse(
        String sistema,
        String status,
        int scoreAderencia,
        String conclusao,
        List<String> pendencias
) {
    public PjbSubstituicaoLegadosSistemaResponse {
        sistema = sistema == null ? "" : sistema.trim();
        status = status == null ? "PENDENTE" : status.trim();
        conclusao = conclusao == null ? "" : conclusao.trim();
        pendencias = pendencias == null ? List.of() : List.copyOf(pendencias);
    }
}
