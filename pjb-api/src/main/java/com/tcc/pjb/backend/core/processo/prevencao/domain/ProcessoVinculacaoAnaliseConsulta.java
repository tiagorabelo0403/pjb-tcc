package com.tcc.pjb.backend.core.processo.prevencao.domain;

import java.util.Objects;

public record ProcessoVinculacaoAnaliseConsulta(
        Long processoId,
        String numeroProcesso,
        String solicitante,
        String origemSolicitacao
) {
    public ProcessoVinculacaoAnaliseConsulta {
        numeroProcesso = normalizeNullable(numeroProcesso);
        solicitante = normalizeNullable(solicitante);
        origemSolicitacao = normalizeNullable(origemSolicitacao);
        if (processoId == null && numeroProcesso == null) {
            throw new IllegalArgumentException("a análise de vinculação exige processoId ou número do processo");
        }
    }

    private static String normalizeNullable(String value) {
        String normalized = Objects.toString(value, "").trim();
        return normalized.isBlank() ? null : normalized;
    }
}
