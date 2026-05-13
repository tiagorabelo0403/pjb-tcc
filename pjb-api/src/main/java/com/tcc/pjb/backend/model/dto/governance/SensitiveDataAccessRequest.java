package com.tcc.pjb.backend.model.dto.governance;

import jakarta.validation.constraints.NotNull;

public record SensitiveDataAccessRequest(
        @NotNull Long processoId,
        boolean acessoExcepcional,
        boolean stepUpAtivo,
        boolean justificativaRegistrada,
        boolean duplaAprovacaoAtiva,
        String finalidadeDeclarada
) {
}
