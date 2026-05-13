package com.tcc.pjb.backend.model.dto.profile.operational;

import jakarta.validation.constraints.PositiveOrZero;

public record TransitoConsolidacaoFechamentoExecutivoRequest(
        String modoFechamento,
        String preferencia,
        String subrogacao,
        @PositiveOrZero double percentualSatisfeito,
        @PositiveOrZero double saldoRemanescente,
        String motivo
) {}
