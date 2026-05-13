package com.tcc.pjb.backend.model.dto.profile.operational;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

public record TransitoVinculoArquivamentoTerminalRequest(
        @NotBlank String operacao,
        String disposicaoTerminal,
        String motivo,
        @DecimalMin("0.0") @DecimalMax("100.0") double percentualSatisfeito,
        @PositiveOrZero double saldoRemanescente
) {}
