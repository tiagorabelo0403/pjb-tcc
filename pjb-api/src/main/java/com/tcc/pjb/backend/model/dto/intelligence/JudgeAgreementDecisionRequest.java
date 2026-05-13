package com.tcc.pjb.backend.model.dto.intelligence;

public record JudgeAgreementDecisionRequest(
        String action,
        String justification,
        String hashAssinaturaJuiz,
        boolean notifyParties
) {
}
