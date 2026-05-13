package com.tcc.pjb.backend.core.prazos.auditoria.domain;

public record PrazoWindowAuditView(
        String reference,
        String status,
        String summary
) {
}
