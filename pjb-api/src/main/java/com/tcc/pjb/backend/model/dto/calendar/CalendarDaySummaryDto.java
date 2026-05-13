package com.tcc.pjb.backend.model.dto.calendar;

import java.time.LocalDate;
import java.util.List;

public record CalendarDaySummaryDto(
    LocalDate date,
    boolean hasEvents,
    int totalEvents,
    List<String> colors,
    boolean marked
) {
}
