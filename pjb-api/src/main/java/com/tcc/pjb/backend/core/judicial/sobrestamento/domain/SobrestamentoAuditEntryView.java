package com.tcc.pjb.backend.core.judicial.sobrestamento.domain;

import java.time.Instant;

public record SobrestamentoAuditEntryView(
        String code,
        String status,
        String detail,
        Instant capturedAt,
        Long referenceId
) {
}
