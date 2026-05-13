package com.tcc.pjb.backend.model.dto.processual.rollout;

import jakarta.validation.constraints.NotBlank;
import com.tcc.pjb.backend.integration.judicial.JudicialSystem;

public record NationalFeatureRolloutRequest(
        @NotBlank String featureCode,
        Long processoId,
        String tribunalCodigo,
        JudicialSystem judicialSystem,
        String targetProfile,
        String uf,
        String comarca,
        Integer rolloutPercentOverride
) {
}
