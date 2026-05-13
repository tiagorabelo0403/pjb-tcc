package com.tcc.pjb.backend.model.dto.secretariat.queue;

import java.time.Instant;

public record SecretariatQueueAgendaCompletionDto(
    String eventCode,
    String status,
    Instant occurredAt,
    boolean autoReturnReady,
    String returnRoute
) {
}
