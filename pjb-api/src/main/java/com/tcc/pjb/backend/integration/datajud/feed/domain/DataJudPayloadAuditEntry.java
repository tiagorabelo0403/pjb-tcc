package com.tcc.pjb.backend.integration.datajud.feed.domain;

public record DataJudPayloadAuditEntry(
        String reference,
        String stage,
        String detail
) {
}
