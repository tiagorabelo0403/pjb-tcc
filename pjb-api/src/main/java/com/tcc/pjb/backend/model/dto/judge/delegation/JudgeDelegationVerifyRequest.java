package com.tcc.pjb.backend.model.dto.judge.delegation;


public record JudgeDelegationVerifyRequest(
        String token,
        String deviceBindingHash
) {
}
