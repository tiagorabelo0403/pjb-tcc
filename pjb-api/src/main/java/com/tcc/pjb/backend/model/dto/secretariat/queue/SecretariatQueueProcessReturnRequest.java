package com.tcc.pjb.backend.model.dto.secretariat.queue;

import java.time.Instant;

public record SecretariatQueueProcessReturnRequest(
    String processReturnStatus,
    Instant returnedAt,
    String note
) {
}
