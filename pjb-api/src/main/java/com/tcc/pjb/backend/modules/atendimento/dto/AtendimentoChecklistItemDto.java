package com.tcc.pjb.backend.modules.atendimento.dto;

import java.time.Instant;

public record AtendimentoChecklistItemDto(
    Long id,
    Long threadId,
    String kind,
    String status,
    String title,
    String note,
    Instant dueAt,
    Long documentoId,
    Long createdByUserId,
    Instant createdAt,
    Instant updatedAt,
    Instant completedAt,
    Long completedByUserId,
    Instant cancelledAt,
    Long cancelledByUserId
) {
}
