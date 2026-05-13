package com.tcc.pjb.backend.model.dto.intelligence;

import java.util.List;

public record ExecutionRecoveryRiskResponse(
        Long processoId,
        double recoveryProbability,
        String recoverabilityBand,
        double confidence,
        int relatedDebtorCases,
        List<String> assetSignals,
        List<String> fundamentos,
        List<String> recommendedStrategies
) {
}
