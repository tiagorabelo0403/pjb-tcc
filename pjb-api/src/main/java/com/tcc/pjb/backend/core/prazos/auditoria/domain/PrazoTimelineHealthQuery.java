package com.tcc.pjb.backend.core.prazos.auditoria.domain;

public record PrazoTimelineHealthQuery(
        String reference,
        String scope,
        Integer limit
) {
}
