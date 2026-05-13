package com.tcc.pjb.backend.model.dto.ministro;

import jakarta.validation.constraints.NotBlank;

public record MinistroDecisaoPlenariaRequest(
        @NotBlank String votacao,
        @NotBlank String ementa,
        @NotBlank String dispositivo
) {}
