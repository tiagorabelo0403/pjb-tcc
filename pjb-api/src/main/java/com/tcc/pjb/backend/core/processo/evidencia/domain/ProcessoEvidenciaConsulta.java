package com.tcc.pjb.backend.core.processo.evidencia.domain;

import java.util.Objects;
import java.util.UUID;

public record ProcessoEvidenciaConsulta(
        Long processoId,
        String numeroProcesso,
        UUID documentoId,
        boolean incluirCorrelatos,
        boolean incluirGrafo,
        String solicitante,
        String origemSolicitacao
) {
    public ProcessoEvidenciaConsulta {
        numeroProcesso = normalizeNullable(numeroProcesso);
        solicitante = normalizeNullable(solicitante);
        origemSolicitacao = normalizeNullable(origemSolicitacao);
        Objects.requireNonNull(documentoId, "documentoId");
        if (processoId == null && numeroProcesso == null) {
            throw new IllegalArgumentException("a consulta de evidência exige processoId ou número do processo");
        }
    }

    private static String normalizeNullable(String value) {
        String normalized = Objects.toString(value, "").trim();
        return normalized.isBlank() ? null : normalized;
    }
}
