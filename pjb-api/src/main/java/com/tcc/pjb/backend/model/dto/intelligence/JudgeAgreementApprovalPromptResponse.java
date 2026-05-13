package com.tcc.pjb.backend.model.dto.intelligence;

import java.util.List;

public record JudgeAgreementApprovalPromptResponse(
        Long processoId,
        Long propostaId,
        boolean shouldNotifyJudge,
        String queueCode,
        String inboxKey,
        String approvalQuestion,
        List<String> decisionOptions,
        List<String> safeguards,
        List<String> fundamentos,
        String dispatchStatus,
        List<String> notifiedRecipients
) {
}
