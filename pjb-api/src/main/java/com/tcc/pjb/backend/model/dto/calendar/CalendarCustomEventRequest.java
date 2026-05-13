package com.tcc.pjb.backend.model.dto.calendar;

import java.time.LocalDateTime;

public record CalendarCustomEventRequest(
    String title,
    LocalDateTime at,
    String color,
    Long processoId
) {
}
