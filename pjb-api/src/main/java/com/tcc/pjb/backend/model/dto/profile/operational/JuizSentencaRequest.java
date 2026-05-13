package com.tcc.pjb.backend.model.dto.profile.operational;

import jakarta.validation.constraints.NotBlank;

public record JuizSentencaRequest(
        @NotBlank String dispositivo,
        @NotBlank String fundamentacao,
        @NotBlank String tipoSentenca
) {}
