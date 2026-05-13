package com.tcc.pjb.backend.model.dto.security;

import java.time.LocalDateTime;

public record OperationalStepUpChallengeResponse(
        Long challengeId,
        String challengeType,
        String deliveryChannel,
        String actionCode,
        LocalDateTime expiresAt,
        String message,
        String verificationPath,
        boolean signatureRequired
) {
}
