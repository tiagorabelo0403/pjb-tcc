package com.tcc.pjb.backend.model.dto.profile.operational;

import jakarta.validation.constraints.NotBlank;

public record DesembargadorAcordaoRequest(
        @NotBlank String ementa,
        @NotBlank String dispositivo,
        @NotBlank String fundamentacao
) {}
