package com.tcc.pjb.backend.model.dto.advogado;

import io.swagger.v3.oas.annotations.media.Schema;

public final class AdvogadoAuditDto {

    private AdvogadoAuditDto() {
    }

    public record LedgerEventResponse(
            Long id,
            @Schema(description = "Data/hora de criação do evento de auditoria", format = "date-time",
                    example = "2026-06-01T10:00:00-03:00") String createdAt,
            String action,
            String resourceType,
            String resourceId,
            String requestId,
            String payloadHash,
            String entryHash
    ) {
    }
}
