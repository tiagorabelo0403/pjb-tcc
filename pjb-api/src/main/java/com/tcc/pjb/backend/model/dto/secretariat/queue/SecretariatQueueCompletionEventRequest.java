package com.tcc.pjb.backend.model.dto.secretariat.queue;

import jakarta.validation.constraints.NotBlank;
import java.time.Instant;

public record SecretariatQueueCompletionEventRequest(
    @NotBlank String eventCode,
    String completionEventStatus,
    Instant occurredAt,
    String note
) {
}
