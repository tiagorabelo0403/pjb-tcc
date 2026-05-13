package com.tcc.pjb.backend.model.dto.intelligence;

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
            String createdAt
    ) {
    }
}
