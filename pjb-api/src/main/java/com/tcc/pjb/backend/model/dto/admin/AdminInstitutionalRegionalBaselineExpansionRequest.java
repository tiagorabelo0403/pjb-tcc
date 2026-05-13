package com.tcc.pjb.backend.model.dto.admin;

import jakarta.validation.constraints.NotBlank;

public record AdminInstitutionalRegionalBaselineExpansionRequest(
        @NotBlank String uf,
        @NotBlank String comarca,
        String foro
) {
}
