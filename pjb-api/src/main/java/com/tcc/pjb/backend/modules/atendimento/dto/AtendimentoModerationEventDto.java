package com.tcc.pjb.backend.modules.atendimento.dto;

import java.time.Instant;

public record AtendimentoModerationEventDto(
    Long id,
    Instant createdAt,
    Long actorUserId,
    String actorTipo,
    Long threadId,
    Long processoId,
    String reason
) {
}
