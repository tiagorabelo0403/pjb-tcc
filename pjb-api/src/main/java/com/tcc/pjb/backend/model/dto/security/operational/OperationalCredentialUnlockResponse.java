package com.tcc.pjb.backend.model.dto.security.operational;

import java.time.LocalDateTime;
import java.util.Map;

public record OperationalCredentialUnlockResponse(
        String functionCode,
        String actionCode,
        String referenceId,
        String unlockToken,
        LocalDateTime expiresAt,
        Map<String, Object> metadata
) {
}
