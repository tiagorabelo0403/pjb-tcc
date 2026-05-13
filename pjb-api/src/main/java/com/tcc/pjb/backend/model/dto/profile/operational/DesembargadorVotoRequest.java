package com.tcc.pjb.backend.model.dto.profile.operational;

import jakarta.validation.constraints.NotBlank;

public record DesembargadorVotoRequest(
        @NotBlank String voto,
        @NotBlank String fundamentacao,
        @NotBlank String decisao
) {}
