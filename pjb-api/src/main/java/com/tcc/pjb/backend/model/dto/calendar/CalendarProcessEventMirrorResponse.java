package com.tcc.pjb.backend.model.dto.calendar;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record CalendarProcessEventMirrorResponse(
        Instant generatedAt,
        Long processoId,
        String processoNumero,
        String tribunal,
        String comarca,
        String rito,
        List<LinkedCalendarEventDto> calendarEvents,
        List<LinkedTimelineItemDto> timelineItems,
        List<DayMirrorDto> days
) {
    public record LinkedCalendarEventDto(
            Long eventId,
            String laneCode,
            String segmentCode,
            String presentationCode,
            String detailCode,
            String iconCode,
            int attentionScore,
            String title,
            String subtitle,
            LocalDateTime at,
            String color,
            String detailsUrl,
            String deadlineRuleSummary
    ) {
    }

    public record LinkedTimelineItemDto(
            Long id,
            Instant data,
            String descricao,
            String faseDe,
            String fasePara,
            String matchedColor,
            String matchedEventTitle,
            String matchedPresentationCode,
            String matchedIconCode,
            int matchedAttentionScore,
            String matchReason
    ) {
    }

    public record DayMirrorDto(
            LocalDate day,
            String dominantColor,
            int totalEvents,
            List<String> titles
    ) {
    }
}
