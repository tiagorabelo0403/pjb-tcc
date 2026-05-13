package com.tcc.pjb.backend.model.dto.secretariat.queue;

import java.time.Instant;

public record SecretariatQueueVenueConfirmationRequest(
    String modality,
    String locationLabel,
    String roomLabel,
    String virtualLink,
    String confirmationStatus,
    Instant confirmedAt,
    String note
) {
}
