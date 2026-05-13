package com.tcc.pjb.backend.model.dto.calendar;

import java.time.LocalDate;
import java.util.List;

public record CalendarEventsResponse(
        LocalDate from,
        LocalDate to,
        List<CalendarDayDto> days
) {
  public record CalendarDayDto(LocalDate day, List<CalendarEventDto> events) {
  }
}
