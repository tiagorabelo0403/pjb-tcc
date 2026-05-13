package com.tcc.pjb.backend.model.dto.judge.delegation;

public record JudgeDelegationFlowRequest(
        Long magistrateId,
        Integer duracaoMinutos,
        String scope,
        String deviceBindingHash,
        String motivo
) {
}
