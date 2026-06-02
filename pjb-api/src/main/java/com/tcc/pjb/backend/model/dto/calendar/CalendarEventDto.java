package com.tcc.pjb.backend.model.dto.calendar;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record CalendarEventDto(
        String eventType,
        Long eventId,
        Long processoId,
        String processoNumero,
        String title,
        @Schema(description = "Data e hora do evento no calendário", format = "date-time",
                example = "2026-06-01T14:00:00-03:00") LocalDateTime at,
        String color,
        boolean marked,
        String detailsUrl,
        String body,
        String domainKey,
        String sourceCode
) {

    public LocalDateTime start() {
        return at;
    }
}
