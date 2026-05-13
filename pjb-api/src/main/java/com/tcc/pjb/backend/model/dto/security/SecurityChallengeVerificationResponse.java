package com.tcc.pjb.backend.model.dto.security;

public record SecurityChallengeVerificationResponse(
        boolean ok,
        String code,
        String message,
        SecurityChallengeVerificationDetailsResponse details
) {
}
