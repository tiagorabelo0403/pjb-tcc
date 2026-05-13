package com.tcc.pjb.backend.core.security.stepup;


public record FaceReauthTokenPayload(
        String jti,
        Long userId,
        long iat,
        long exp,
        String level
) {
}
