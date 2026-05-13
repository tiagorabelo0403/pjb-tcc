package com.tcc.pjb.backend.model.dto.profile.operational;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

public record TransitoLiquidacaoProdutoExpropriacaoRequest(
        @NotBlank String bem,
        String modoProduto,
        String preferencia,
        String subrogacao,
        @PositiveOrZero double valorProduto,
        @PositiveOrZero double saldoExecutado,
        @PositiveOrZero double saldoCredor
) {}
