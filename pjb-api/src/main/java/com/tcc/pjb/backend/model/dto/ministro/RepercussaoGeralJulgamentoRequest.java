package com.tcc.pjb.backend.model.dto.ministro;

import jakarta.validation.constraints.NotBlank;

public record RepercussaoGeralJulgamentoRequest(
        @NotBlank String teseFirmada,
        String efeitosProcessuais
) {
}
