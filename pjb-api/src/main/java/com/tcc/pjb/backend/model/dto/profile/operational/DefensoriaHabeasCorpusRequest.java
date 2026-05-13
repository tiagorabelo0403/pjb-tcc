package com.tcc.pjb.backend.model.dto.profile.operational;

import jakarta.validation.constraints.NotBlank;

public record DefensoriaHabeasCorpusRequest(
        @NotBlank String impetrante,
        @NotBlank String paciente,
        @NotBlank String fundamentacao
) {}
