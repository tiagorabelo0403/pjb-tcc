package com.tcc.pjb.backend.integration.mni.domain;

import java.time.Instant;

public record MniAuditEntryView(
        String code,
        String status,
        String detail,
        Instant capturedAt,
        Long referenceId
) {
}
