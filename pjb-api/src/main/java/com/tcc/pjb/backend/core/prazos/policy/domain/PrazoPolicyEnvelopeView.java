package com.tcc.pjb.backend.core.prazos.policy.domain;

import java.time.Instant;

public record PrazoPolicyEnvelopeView(
        String code,
        String status,
        String detail,
        Instant capturedAt,
        Long referenceId
) {
}
