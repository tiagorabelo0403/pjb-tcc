package com.tcc.pjb.backend.model.dto.advogado;

public final class AdvogadoAuditDto {

    private AdvogadoAuditDto() {
    }

    public record LedgerEventResponse(
            Long id,
            String createdAt,
            String action,
            String resourceType,
            String resourceId,
            String requestId,
            String payloadHash,
            String entryHash
    ) {
    }
}
