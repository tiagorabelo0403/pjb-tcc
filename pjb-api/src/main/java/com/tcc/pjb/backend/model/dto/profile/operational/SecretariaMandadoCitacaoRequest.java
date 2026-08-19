package com.tcc.pjb.backend.model.dto.profile.operational;

import jakarta.validation.constraints.NotBlank;

public record SecretariaMandadoCitacaoRequest(
        Long oficialId,
        @NotBlank String enderecoCitacao,
        String observacaoOperacional
) {}
