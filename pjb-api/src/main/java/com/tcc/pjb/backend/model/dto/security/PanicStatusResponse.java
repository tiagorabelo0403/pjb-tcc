package com.tcc.pjb.backend.model.dto.security;

import java.time.LocalDateTime;

public record PanicStatusResponse(
        boolean frozen,
        LocalDateTime frozenAt,
        LocalDateTime frozenUntil,
        String reason
) {
}
