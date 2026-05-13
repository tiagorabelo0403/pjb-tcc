package com.tcc.pjb.backend.model.dto.security;

public record WebAuthnEnrollmentChallengeResponse(
        Long sessionId,
        String optionsJson
) {
}
