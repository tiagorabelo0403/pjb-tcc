package com.tcc.pjb.backend.model.dto.security.context;

import java.time.LocalDateTime;

public record SecurityDeviceResponse(Long deviceId,
                                     String alias,
                                     boolean verified,
                                     boolean attestationTrusted,
                                     LocalDateTime quarantineUntil,
                                     Long pendingChallengeId,
                                     String pendingChallengeType,
                                     String pendingChallengeHint) {
}
