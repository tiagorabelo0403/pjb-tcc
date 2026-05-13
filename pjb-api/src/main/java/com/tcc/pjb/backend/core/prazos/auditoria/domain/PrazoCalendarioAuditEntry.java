package com.tcc.pjb.backend.core.prazos.auditoria.domain;

import java.time.Instant;

public record PrazoCalendarioAuditEntry(
        String code,
        String status,
        String detail,
        Instant capturedAt,
        Long referenceId
) {
}
