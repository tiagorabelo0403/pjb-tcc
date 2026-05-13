package com.tcc.pjb.backend.model.dto.profile.operational;

import jakarta.validation.constraints.NotBlank;

public record JuizDespachoRequest(
        @NotBlank String conteudo,
        @NotBlank String fundamentacao
) {}
