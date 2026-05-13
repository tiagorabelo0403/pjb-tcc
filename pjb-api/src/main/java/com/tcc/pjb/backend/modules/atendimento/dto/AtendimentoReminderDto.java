package com.tcc.pjb.backend.modules.atendimento.dto;

import java.time.Instant;

public record AtendimentoReminderDto(
    Long id,
    Long threadId,
    Long createdByUserId,
    Long targetUserId,
    String body,
    Instant fireAt,
    String status,
    int attempts,
    String lastError,
    Long sentMessageId,
    Instant createdAt,
    Instant updatedAt
) {
}
