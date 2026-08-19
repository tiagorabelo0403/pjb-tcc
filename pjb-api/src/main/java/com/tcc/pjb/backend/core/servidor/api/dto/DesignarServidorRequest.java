package com.tcc.pjb.backend.core.servidor.api.dto;

import com.tcc.pjb.backend.model.entity.enums.FuncaoServidorJudiciario;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record DesignarServidorRequest(
        @NotNull Long usuarioId,
        @NotNull Long unidadeId,
        @NotNull FuncaoServidorJudiciario funcao,
        @NotNull LocalDate dataInicio,
        String portaria
) {
}
