package com.tcc.pjb.backend.model.dto.security;

import java.time.LocalDateTime;

public record PanicTriggerResponse(
        LocalDateTime frozenAt,
        LocalDateTime frozenUntil,
        String reason,
        Long deviceId
) {
}
