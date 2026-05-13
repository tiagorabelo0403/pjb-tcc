package com.tcc.pjb.backend.model.dto.judge.delegation;

import java.time.Instant;

public record JudgeDelegationFlowView(
        Long id,
        String requestUuid,
        Long magistrateId,
        String magistrateNome,
        Long delegateId,
        String delegateNome,
        String scope,
        String status,
        Integer durationMinutes,
        String motivo,
        String deviceBindingHash,
        Instant requestedAt,
        Instant approvedAt,
        Instant rejectedAt,
        Instant revokedAt,
        Instant expiresAt,
        String tokenJti
) {
}
