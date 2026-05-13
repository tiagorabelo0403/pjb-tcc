package com.tcc.pjb.backend.core.security.domain;

import java.time.Instant;

public record GovBrAssuranceEnvelopeView(
        String code,
        String status,
        String detail,
        Instant capturedAt,
        Long referenceId
) {
}
