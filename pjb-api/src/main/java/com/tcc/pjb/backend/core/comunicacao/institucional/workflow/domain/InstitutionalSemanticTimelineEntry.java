package com.tcc.pjb.backend.core.comunicacao.institucional.workflow.domain;

import java.time.Instant;

public record InstitutionalSemanticTimelineEntry(
        String eventId,
        String icone,
        String titulo,
        String descricao,
        String faseSemantica,
        Instant occurredAt
) {
}
