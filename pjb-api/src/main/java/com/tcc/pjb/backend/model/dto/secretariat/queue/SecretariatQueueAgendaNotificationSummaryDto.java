package com.tcc.pjb.backend.model.dto.secretariat.queue;

public record SecretariatQueueAgendaNotificationSummaryDto(
    String status,
    long readyCount,
    long pendingCount,
    long missingCount
) {
}
