package com.tcc.pjb.backend.model.dto.calendar;

public record CalendarMarkerRequest(
        String eventType,
        Long eventId,
        String color
) {
}
