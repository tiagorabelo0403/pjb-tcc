package com.tcc.pjb.backend.model.dto.governance;

import jakarta.validation.constraints.NotNull;
import com.tcc.pjb.backend.model.entity.enums.OperacaoProcessualCritica;

public record FunctionalAuthorityEvaluationRequest(
        @NotNull Long processoId,
        @NotNull OperacaoProcessualCritica operacao,
        boolean stepUpAtivo,
        boolean duplaAprovacaoAtiva,
        boolean revisaoIndependenteAtiva,
        boolean justificativaRegistrada,
        String finalidadeDeclarada
) {
}
