package com.tcc.pjb.backend.integration.mni.domain;

public record MniProtocolAuditEntry(
        String reference,
        String stage,
        String detail
) {
}
