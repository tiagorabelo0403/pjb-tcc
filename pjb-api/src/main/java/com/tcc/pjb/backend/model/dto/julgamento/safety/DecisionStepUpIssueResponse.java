package com.tcc.pjb.backend.model.dto.julgamento.safety;

public record DecisionStepUpIssueResponse(
        String token,
        long expiresAtEpoch,
        long ttlSeconds,
        Long focusSessionId,
        Long processoId,
        String actType,
        String descriptorCode,
        String securityAction
) {}
