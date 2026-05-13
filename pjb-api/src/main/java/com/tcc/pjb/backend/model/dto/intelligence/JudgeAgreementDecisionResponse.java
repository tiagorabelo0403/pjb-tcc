package com.tcc.pjb.backend.model.dto.intelligence;

import java.util.List;

public record JudgeAgreementDecisionResponse(
        Long processoId,
        Long propostaId,
        String action,
        String statusAcordo,
        String workItemStatus,
        String nextStep,
        String chatMessage,
        List<String> notifiedRecipients
) {
}
