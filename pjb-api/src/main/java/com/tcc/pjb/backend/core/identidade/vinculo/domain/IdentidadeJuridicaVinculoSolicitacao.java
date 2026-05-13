package com.tcc.pjb.backend.core.identidade.vinculo.domain;

import java.util.Objects;

public record IdentidadeJuridicaVinculoSolicitacao(
        Long processoId,
        String numeroProcesso,
        boolean persistirNoGrafo,
        String solicitante,
        String origemSolicitacao
) {
    public IdentidadeJuridicaVinculoSolicitacao {
        numeroProcesso = normalizeNullable(numeroProcesso);
        solicitante = normalizeNullable(solicitante);
        origemSolicitacao = normalizeNullable(origemSolicitacao);
        if (processoId == null && numeroProcesso == null) {
            throw new IllegalArgumentException("a solicitação de vínculo exige processoId ou número do processo");
        }
    }

    private static String normalizeNullable(String value) {
        String normalized = Objects.toString(value, "").trim();
        return normalized.isBlank() ? null : normalized;
    }
}
