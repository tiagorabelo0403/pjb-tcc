package com.tcc.pjb.backend.model.dto.calendar;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

public record CalendarNotificationPreviewResponse(
        Instant generatedAt,
        Long usuarioId,
        String profileCode,
        String activeInstitutionContextCode,
        int totalPending,
        int criticalPending,
        long unreadInbox,
        List<CalendarNotificationItemDto> items
) {
    public record CalendarNotificationItemDto(
            String notificationKey,
            String stageCode,
            String urgency,
            String laneCode,
            String segmentCode,
            String presentationCode,
            String iconCode,
            int attentionScore,
            Long processoId,
            String processoNumero,
            String title,
            String body,
            @Schema(description = "Data e hora do evento da notificação", format = "date-time",
                    example = "2026-06-01T14:00:00-03:00") LocalDateTime eventAt,
            @Schema(description = "Data e hora de envio da notificação", format = "date-time",
                    example = "2026-06-01T13:45:00-03:00") LocalDateTime notifyAt,
            String color,
            String detailsUrl,
            String audienceCode,
            String windowLabel,
            String cadenceMode
    ) {
    }
}
