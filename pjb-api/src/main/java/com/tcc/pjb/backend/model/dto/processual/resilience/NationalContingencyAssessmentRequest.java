package com.tcc.pjb.backend.model.dto.processual.resilience;

import jakarta.validation.constraints.NotNull;
import com.tcc.pjb.backend.integration.judicial.JudicialSystem;

public record NationalContingencyAssessmentRequest(
        @NotNull Long processoId,
        JudicialSystem judicialSystem,
        boolean requireSubmission,
        boolean requireSync,
        boolean forceOficialFallback,
        String finalidade
) {
}
