package com.tcc.pjb.backend.model.dto.profile.operational;

import jakarta.validation.constraints.NotBlank;

public record SecretariaJuntadaRequest(
        @NotBlank String tipoDocumento,
        @NotBlank String descricao,
        @NotBlank String origem
) {}
