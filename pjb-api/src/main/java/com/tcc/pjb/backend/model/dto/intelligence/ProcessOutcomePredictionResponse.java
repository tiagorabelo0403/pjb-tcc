package com.tcc.pjb.backend.model.dto.intelligence;

import java.util.List;

public record ProcessOutcomePredictionResponse(
        Long processoId,
        String predictedDisposition,
        String recommendationBand,
        double procedenciaProbabilidade,
        double procedenciaParcialProbabilidade,
        double improcedenciaProbabilidade,
        double acordoProbabilidade,
        double confidence,
        String judgeProfileTendency,
        double judgeHomologationRate,
        List<String> preferredClauses,
        List<String> fundamentos,
        List<String> conciliacaoPrompts
) {
}
