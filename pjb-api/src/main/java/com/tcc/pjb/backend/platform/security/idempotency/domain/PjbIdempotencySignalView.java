package com.tcc.pjb.backend.platform.security.idempotency.domain;

import java.time.Instant;

public record PjbIdempotencySignalView(
        String code,
        String status,
        String detail,
        Instant capturedAt,
        Long referenceId
) {
}
