package com.tcc.pjb.backend.model.dto.processual.calculo;

import jakarta.validation.constraints.NotBlank;

public record CalculoJudicialExperiencePreferenceRequest(
        @NotBlank String experienceMode,
        String domainCode,
        String ramoDireito,
        String classeProcessual,
        String tipoCausa,
        String perfilEquipe,
        String tribunal,
        String sistemaOrigem,
        boolean persistForTeam,
        boolean institutionalPolicy
) {
}
