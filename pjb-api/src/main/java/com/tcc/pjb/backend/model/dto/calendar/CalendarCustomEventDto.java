package com.tcc.pjb.backend.model.dto.calendar;

import java.time.LocalDateTime;

public record CalendarCustomEventDto(
    Long id,
    String title,
    LocalDateTime at,
    String color,
    Long processoId,
    String processoNumero
) {
}
