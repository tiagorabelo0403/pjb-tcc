package com.tcc.pjb.backend.model.dto.calendar;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record CalendarCustomEventDto(
    Long id,
    String title,
    @Schema(description = "Data e hora do evento personalizado", format = "date-time",
            example = "2026-06-01T14:00:00-03:00") LocalDateTime at,
    String color,
    Long processoId,
    String processoNumero
) {
}
