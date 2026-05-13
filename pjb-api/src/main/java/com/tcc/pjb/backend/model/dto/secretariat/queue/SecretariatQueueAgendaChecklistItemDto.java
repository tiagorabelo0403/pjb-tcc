package com.tcc.pjb.backend.model.dto.secretariat.queue;

import java.time.Instant;

public record SecretariatQueueAgendaChecklistItemDto(
    String code,
    String label,
    String status,
    boolean blocking,
    Instant confirmedAt
) {
}
