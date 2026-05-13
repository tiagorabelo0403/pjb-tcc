package com.tcc.pjb.backend.model.dto.calendar;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

public record CalendarNotificationEnvelope(
        UUID notificationId,
        Long usuarioId,
        Long processoId,
        String processoNumero,
        String profileCode,
        String laneCode,
        String segmentCode,
        String stageCode,
        String urgency,
        String color,
        String title,
        String body,
        String detailsUrl,
        String audienceCode,
        LocalDateTime eventAt,
        LocalDateTime notifyAt,
        Instant generatedAt,
        String notificationKey
) {
}
