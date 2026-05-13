package com.tcc.pjb.backend.model.dto.govbr;

import java.time.Instant;

public record GovBrStepUpStartResponse(String authorizeUrl, Instant expiresAt) {
}
