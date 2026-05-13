package com.tcc.pjb.backend.model.dto.judge.delegation;

import java.time.Instant;

public record JudgeDelegationVerifyResponse(
        boolean valid,
        String jti,
        Long magistrateId,
        Long delegateId,
        String scope,
        Instant expiresAt
) {
}
