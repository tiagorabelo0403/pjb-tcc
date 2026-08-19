package com.tcc.pjb.backend.core.servidor.api.dto;

import com.tcc.pjb.backend.model.entity.enums.FuncaoServidorJudiciario;
import jakarta.validation.constraints.NotNull;

public record FuncaoServidorSolicitacaoCreateRequest(
        @NotNull Long unidadeId,
        @NotNull FuncaoServidorJudiciario funcao,
        String motivo
) {
}
