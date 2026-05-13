package com.tcc.pjb.backend.model.dto.profile.operational;

import jakarta.validation.constraints.NotBlank;

public record OperationalMinistroDecisaoPlenariaRequest(
        @NotBlank String votacao,
        @NotBlank String ementa,
        @NotBlank String dispositivo
) {}
