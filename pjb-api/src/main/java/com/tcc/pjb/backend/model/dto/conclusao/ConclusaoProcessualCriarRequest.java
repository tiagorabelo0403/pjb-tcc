package com.tcc.pjb.backend.model.dto.conclusao;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ConclusaoProcessualCriarRequest(
        @NotNull Long processoId,
        @NotNull Long magistradoId,
        @NotBlank String tipoConclusao,
        String motivo
) {
}
