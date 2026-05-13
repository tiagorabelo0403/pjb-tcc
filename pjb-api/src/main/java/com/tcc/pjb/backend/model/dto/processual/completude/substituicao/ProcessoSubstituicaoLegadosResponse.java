package com.tcc.pjb.backend.model.dto.processual.completude.substituicao;

import java.time.Instant;
import java.util.List;

public record ProcessoSubstituicaoLegadosResponse(
        Long processoId,
        String numeroProcesso,
        int scoreGeral,
        boolean prontoSubstituicaoImediata,
        String conclusaoTecnica,
        List<ProcessoSubstituicaoLegadosProvaResponse> provas,
        List<ProcessoSubstituicaoLegadosSistemaResponse> sistemas,
        List<String> fundamentos,
        Instant geradoEm
) {
    public ProcessoSubstituicaoLegadosResponse {
        numeroProcesso = numeroProcesso == null ? "" : numeroProcesso;
        provas = provas == null ? List.of() : List.copyOf(provas);
        sistemas = sistemas == null ? List.of() : List.copyOf(sistemas);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
        geradoEm = geradoEm == null ? Instant.now() : geradoEm;
    }
}
