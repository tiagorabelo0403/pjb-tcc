package com.tcc.pjb.backend.model.dto.judge.delegation;


public record JudgeDelegationIssueRequest(
        Long assessorId,
        Integer duracaoMinutos,
        String scope,
        String deviceBindingHash
) {
}
