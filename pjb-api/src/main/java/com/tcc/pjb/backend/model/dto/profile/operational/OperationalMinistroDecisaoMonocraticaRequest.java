package com.tcc.pjb.backend.model.dto.profile.operational;

import jakarta.validation.constraints.NotBlank;

public record OperationalMinistroDecisaoMonocraticaRequest(
        @NotBlank String relatorio,
        @NotBlank String fundamentacao,
        @NotBlank String dispositivo
) {}
