package com.tcc.pjb.backend.core.observability.domain;

import java.time.Instant;

public record PjbSloViolationAuditView(
        String code,
        String status,
        String detail,
        Instant capturedAt,
        Long referenceId
) {
}
