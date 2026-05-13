package com.tcc.pjb.backend.model.dto.governance;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DocumentoVersionamentoRequest(
        @NotNull Long processoId,
        @NotBlank String tituloBase,
        boolean retificacao,
        boolean bloqueadoPorAssinatura
) {
}
