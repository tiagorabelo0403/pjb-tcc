package com.tcc.pjb.backend.core.prazos.auditoria.domain;

public record PrazoCalendarioAuditView(
        String reference,
        String status,
        String summary
) {
}
