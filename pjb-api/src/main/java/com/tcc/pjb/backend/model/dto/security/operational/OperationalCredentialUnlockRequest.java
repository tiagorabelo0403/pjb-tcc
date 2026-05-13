package com.tcc.pjb.backend.model.dto.security.operational;

public record OperationalCredentialUnlockRequest(
        String actionCode,
        String referenceId,
        String password
) {
}
