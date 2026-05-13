package com.tcc.pjb.backend.model.dto.profile.operational;

import jakarta.validation.constraints.NotBlank;

public record ConciliacaoResultadoSessaoRequest(
        @NotBlank String resultado,
        @NotBlank String observacoes,
        boolean acordoFirmado
) {}
