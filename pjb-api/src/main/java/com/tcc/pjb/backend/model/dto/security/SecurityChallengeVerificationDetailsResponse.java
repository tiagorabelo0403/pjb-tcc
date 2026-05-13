package com.tcc.pjb.backend.model.dto.security;

import java.util.List;

public record SecurityChallengeVerificationDetailsResponse(
        String challengeType,
        Long deviceId,
        List<Long> deviceIds,
        String hint
) {
}
