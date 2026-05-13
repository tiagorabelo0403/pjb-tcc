package com.tcc.pjb.backend.model.dto.processual.calculo;

import jakarta.validation.constraints.NotBlank;
import java.util.Map;

public record CalculoJudicialIaFinanceiraCommandRequest(
        @NotBlank String dominio,
        Map<String, Object> payload,
        String pedidoUsuario,
        String executionProfile
) {
}
