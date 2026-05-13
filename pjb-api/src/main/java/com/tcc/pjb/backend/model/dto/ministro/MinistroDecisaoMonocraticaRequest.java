package com.tcc.pjb.backend.model.dto.ministro;

import jakarta.validation.constraints.NotBlank;

public record MinistroDecisaoMonocraticaRequest(
        @NotBlank String relatorio,
        @NotBlank String fundamentacao,
        @NotBlank String dispositivo
) {}
