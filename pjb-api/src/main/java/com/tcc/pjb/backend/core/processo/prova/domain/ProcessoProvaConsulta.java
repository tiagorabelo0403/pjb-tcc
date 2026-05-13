package com.tcc.pjb.backend.core.processo.prova.domain;

import java.util.Objects;
import java.util.UUID;

public record ProcessoProvaConsulta(
        Long processoId,
        String numeroProcesso,
        UUID documentoId,
        String solicitante,
        String origemSolicitacao
) {
    public ProcessoProvaConsulta {
        numeroProcesso = normalizeNullable(numeroProcesso);
        solicitante = normalizeNullable(solicitante);
        origemSolicitacao = normalizeNullable(origemSolicitacao);
        Objects.requireNonNull(documentoId, "documentoId");
        if (processoId == null && numeroProcesso == null) {
            throw new IllegalArgumentException("a análise de prova exige processoId ou número do processo");
        }
    }

    private static String normalizeNullable(String value) {
        String normalized = Objects.toString(value, "").trim();
        return normalized.isBlank() ? null : normalized;
    }
}
