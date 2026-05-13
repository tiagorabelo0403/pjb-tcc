package com.tcc.pjb.backend.model.dto.secretariat.queue;

import java.time.Instant;

public record SecretariatQueueAgendaVenueDto(
    String modality,
    String locationLabel,
    String roomLabel,
    String virtualLink,
    String confirmationStatus,
    boolean confirmed,
    Instant confirmedAt
) {
}
