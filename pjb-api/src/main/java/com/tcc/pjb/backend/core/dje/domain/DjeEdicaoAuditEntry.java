package com.tcc.pjb.backend.core.dje.domain;

public record DjeEdicaoAuditEntry(
        String reference,
        String stage,
        String detail
) {
}
