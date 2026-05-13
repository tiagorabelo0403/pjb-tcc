package com.tcc.pjb.backend.model.dto.profile.operational;

import jakarta.validation.constraints.NotBlank;

public record JuizDecisaoInterlocutoriaRequest(
        @NotBlank String dispositivo,
        @NotBlank String fundamentacao,
        @NotBlank String tipoDecisao
) {}
