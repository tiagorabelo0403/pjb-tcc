package com.tcc.pjb.backend.model.dto.security;

public record BaptismStartResponse(
        Long challengeId,
        String nonceBase64Url,
        String termText
) {
}
