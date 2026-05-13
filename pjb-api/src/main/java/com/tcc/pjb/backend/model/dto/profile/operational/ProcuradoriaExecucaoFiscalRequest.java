package com.tcc.pjb.backend.model.dto.profile.operational;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

public record ProcuradoriaExecucaoFiscalRequest(
        @NotBlank String devedorCpfCnpj,
        @PositiveOrZero double valorDivida,
        @NotBlank String descricao,
        @NotBlank String comarca
) {}
