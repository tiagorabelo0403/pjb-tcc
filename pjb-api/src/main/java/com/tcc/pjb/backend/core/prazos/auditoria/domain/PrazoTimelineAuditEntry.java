package com.tcc.pjb.backend.core.prazos.auditoria.domain;

public record PrazoTimelineAuditEntry(
        String reference,
        String stage,
        String detail
) {
}
