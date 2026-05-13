package com.tcc.pjb.backend.model.dto.security;

public record WebAuthnEnrollmentFinishResponse(
        Long deviceId,
        Long pendingChallengeId
) {
}
