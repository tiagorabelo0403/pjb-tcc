package com.tcc.pjb.backend.model.dto.security;

import java.time.LocalDateTime;

public record WebAuthnAuthenticationResponse(
        String token,
        LocalDateTime expiresAt,
        Long deviceId,
        boolean termosPendentes
) {
}
