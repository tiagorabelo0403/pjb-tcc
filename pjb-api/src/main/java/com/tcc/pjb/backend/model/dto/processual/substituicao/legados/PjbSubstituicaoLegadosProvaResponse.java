package com.tcc.pjb.backend.model.dto.processual.substituicao.legados;

import java.util.List;

public record PjbSubstituicaoLegadosProvaResponse(
        String codigo,
        String titulo,
        String status,
        int score,
        boolean concluida,
        List<String> fundamentos,
        List<String> bloqueios
) {
    public PjbSubstituicaoLegadosProvaResponse {
        codigo = codigo == null ? "" : codigo.trim();
        titulo = titulo == null ? "" : titulo.trim();
        status = status == null ? "PENDENTE" : status.trim();
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
        bloqueios = bloqueios == null ? List.of() : List.copyOf(bloqueios);
    }
}
