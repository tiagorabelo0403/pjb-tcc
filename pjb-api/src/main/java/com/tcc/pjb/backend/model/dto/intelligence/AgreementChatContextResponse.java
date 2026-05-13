package com.tcc.pjb.backend.model.dto.intelligence;

import java.util.List;

public record AgreementChatContextResponse(
        Long processoId,
        Long propostaId,
        String statusAcordo,
        boolean acordoPendenteHomologacao,
        boolean canalNegocialSugerido,
        ProcessOutcomePredictionResponse outcomePrediction,
        QualifiedThemeAlertResponse qualifiedThemes,
        StructuredProcessSummaryResponse structuredSummary,
        ProcessFraudRiskResponse fraudRisk,
        ExecutionRecoveryRiskResponse executionRecoveryRisk,
        JudgeAgreementApprovalPromptResponse judgeApprovalPrompt,
        List<String> guardrails,
        List<String> suggestedPrompts,
        List<String> nextActions,
        List<NegotiationRoundResponse> negotiationRounds,
        List<AgreementChatAttachmentResponse> structuredAttachments,
        List<String> allowedSpeakers,
        String agreementChannelStage,
        boolean judgeDecisionEnabled
) {
}
