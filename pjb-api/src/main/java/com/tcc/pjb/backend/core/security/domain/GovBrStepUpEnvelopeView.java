package com.tcc.pjb.backend.core.security.domain;

import java.time.Instant;

public record GovBrStepUpEnvelopeView(
        String code,
        String status,
        String detail,
        Instant capturedAt,
        Long referenceId
) {
}
