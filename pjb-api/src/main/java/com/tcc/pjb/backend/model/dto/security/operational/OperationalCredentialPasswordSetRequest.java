package com.tcc.pjb.backend.model.dto.security.operational;

public record OperationalCredentialPasswordSetRequest(
        String newPassword,
        String confirmPassword,
        Long challengeId,
        String otpCode,
        String note
) {
}
