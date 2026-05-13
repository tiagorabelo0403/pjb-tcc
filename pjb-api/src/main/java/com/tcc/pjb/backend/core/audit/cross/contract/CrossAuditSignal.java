package com.tcc.pjb.backend.core.audit.cross.contract;


public record CrossAuditSignal(
        String correlationKey,
        String resourceType,
        String resourceId,
        String payloadHash
) {
}
