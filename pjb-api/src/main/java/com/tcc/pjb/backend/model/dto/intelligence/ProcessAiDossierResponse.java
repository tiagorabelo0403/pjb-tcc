package com.tcc.pjb.backend.model.dto.intelligence;

public record ProcessAiDossierResponse(
        Long processoId,
        ProcessOutcomePredictionResponse outcomePrediction,
        QualifiedThemeAlertResponse qualifiedThemes,
        StructuredProcessSummaryResponse structuredSummary,
        ProcessFraudRiskResponse fraudRisk,
        ExecutionRecoveryRiskResponse executionRecoveryRisk,
        JudgeAgreementApprovalPromptResponse judgeAgreementApproval
) {
}
