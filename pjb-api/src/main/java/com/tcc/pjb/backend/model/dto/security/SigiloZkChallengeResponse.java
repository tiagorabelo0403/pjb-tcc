package com.tcc.pjb.backend.model.dto.security;

import java.time.Instant;

public record SigiloZkChallengeResponse(
        String challengeId,
        Long processoId,
        String numeroProcesso,
        String escopo,
        String statement,
        String challengePayload,
        String commitmentHash,
        Instant expiraEm
) {
}
