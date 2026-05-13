package com.tcc.pjb.backend.model.dto.processual.substituicao.legados;

import java.time.Instant;
import java.util.List;

public record PjbSubstituicaoLegadosResponse(
        Long processoId,
        String numeroProcesso,
        int scoreGeral,
        boolean prontoSubstituicaoImediata,
        String conclusaoTecnica,
        List<PjbSubstituicaoLegadosProvaResponse> provas,
        List<PjbSubstituicaoLegadosSistemaResponse> sistemas,
        List<String> fundamentos,
        Instant geradoEm
) {
    public PjbSubstituicaoLegadosResponse {
        numeroProcesso = numeroProcesso == null ? "" : numeroProcesso.trim();
        conclusaoTecnica = conclusaoTecnica == null ? "" : conclusaoTecnica.trim();
        provas = provas == null ? List.of() : List.copyOf(provas);
        sistemas = sistemas == null ? List.of() : List.copyOf(sistemas);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
        geradoEm = geradoEm == null ? Instant.now() : geradoEm;
    }
}
