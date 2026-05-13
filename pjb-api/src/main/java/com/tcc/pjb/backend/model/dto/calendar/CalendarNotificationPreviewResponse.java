package com.tcc.pjb.backend.model.dto.calendar;

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
            LocalDateTime eventAt,
            LocalDateTime notifyAt,
            String color,
            String detailsUrl,
            String audienceCode,
            String windowLabel,
            String cadenceMode
    ) {
    }
}
