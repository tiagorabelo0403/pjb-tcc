package com.tcc.pjb.backend.model.dto.calendar;

import java.time.LocalDateTime;

public record CalendarEventDto(
        String eventType,
        Long eventId,
        Long processoId,
        String processoNumero,
        String title,
        LocalDateTime at,
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
