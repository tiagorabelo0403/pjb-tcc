package com.tcc.pjb.backend.core.processo.custodia.domain;

import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import java.util.Objects;
import java.util.UUID;

public record ProcessoCustodiaComando(
        Long processoId,
        String numeroProcesso,
        UUID documentoId,
        ProcessoCustodiaAcao acao,
        Long processoDestinoId,
        NivelSigilo nivelSigiloRequerido,
        String justificativa,
        String solicitante,
        String origemSolicitacao
) {
    public ProcessoCustodiaComando {
        numeroProcesso = normalizeNullable(numeroProcesso);
        justificativa = normalizeNullable(justificativa);
        solicitante = normalizeNullable(solicitante);
        origemSolicitacao = normalizeNullable(origemSolicitacao);
        Objects.requireNonNull(documentoId, "documentoId");
        Objects.requireNonNull(acao, "acao");
        if (processoId == null && numeroProcesso == null) {
            throw new IllegalArgumentException("o comando de custódia exige processoId ou número do processo");
        }
    }

    private static String normalizeNullable(String value) {
        String normalized = Objects.toString(value, "").trim();
        return normalized.isBlank() ? null : normalized;
    }
}
