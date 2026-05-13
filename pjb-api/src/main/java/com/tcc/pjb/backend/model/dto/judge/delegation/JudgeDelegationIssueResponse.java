package com.tcc.pjb.backend.model.dto.judge.delegation;

import java.time.Instant;

public record JudgeDelegationIssueResponse(
        String token,
        String jti,
        Long magistrateId,
        Long delegateId,
        String scope,
        Instant expiresAt
) {
}