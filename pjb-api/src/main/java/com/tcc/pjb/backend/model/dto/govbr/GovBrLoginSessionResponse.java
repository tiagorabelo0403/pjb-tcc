package com.tcc.pjb.backend.model.dto.govbr;

import java.time.LocalDateTime;

public record GovBrLoginSessionResponse(String token, LocalDateTime expiresAt, boolean termosPendentes) {
}
