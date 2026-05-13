package com.tcc.pjb.backend.model.dto.security;

public record WebAuthnChallengeResponse(
        Long sessionId,
        String optionsJson
) {
}
