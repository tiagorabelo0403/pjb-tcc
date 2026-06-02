package com.tcc.pjb.backend.model.dto.calendar;

import io.swagger.v3.oas.annotations.media.Schema;
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
        @Schema(description = "Data e hora do evento que gerou a notificação", format = "date-time",
                example = "2026-06-01T14:00:00-03:00") LocalDateTime eventAt,
        @Schema(description = "Data e hora programada para envio da notificação", format = "date-time",
                example = "2026-06-01T13:45:00-03:00") LocalDateTime notifyAt,
        @Schema(description = "Instante de geração do envelope", format = "date-time",
                example = "2026-06-01T10:00:00-03:00") Instant generatedAt,
        String notificationKey
) {
}
