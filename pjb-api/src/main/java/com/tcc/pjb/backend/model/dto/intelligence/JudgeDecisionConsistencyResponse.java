package com.tcc.pjb.backend.model.dto.intelligence;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record JudgeDecisionConsistencyResponse(
        boolean available,
        boolean divergenceRisk,
        String currentOrientation,
        String historicalDominantOrientation,
        double consistencyScore,
        List<DecisionReference> references,
        List<String> fundamentos,
        List<String> reviewChecklist
) {
    public record DecisionReference(
            Long draftId,
            Long processoId,
            String processoNumero,
            String orientation,
            String classeProcessual,
            String assunto,
            @Schema(description = "Data/hora de criação da referência de decisão", format = "date-time",
                    example = "2026-06-01T10:00:00-03:00") String createdAt
    ) {
    }
}
