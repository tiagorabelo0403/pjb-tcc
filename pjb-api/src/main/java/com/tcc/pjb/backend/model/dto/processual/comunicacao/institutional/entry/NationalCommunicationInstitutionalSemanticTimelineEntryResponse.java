package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.entry;

import java.time.Instant;

public record NationalCommunicationInstitutionalSemanticTimelineEntryResponse(
        String eventId,
        String icone,
        String titulo,
        String descricao,
        String faseSemantica,
        Instant occurredAt
) {
}
