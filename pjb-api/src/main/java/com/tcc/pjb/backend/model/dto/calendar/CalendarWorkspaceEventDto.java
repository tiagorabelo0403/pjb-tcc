package com.tcc.pjb.backend.model.dto.calendar;

import java.time.LocalDateTime;

public record CalendarWorkspaceEventDto(
        String laneCode,
        String segmentCode,
        String segmentTitle,
        String eventType,
        Long eventId,
        Long processoId,
        String processoNumero,
        String title,
        String subtitle,
        LocalDateTime at,
        String color,
        boolean marked,
        String detailsUrl,
        String deadlineRuleSummary,
        String audienceCode
) {
}
