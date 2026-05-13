package com.tcc.pjb.backend.core.prazos.auditoria.domain;

public record PrazoTimelineHealthResult(
        boolean available,
        String summary,
        Long total
) {
}
