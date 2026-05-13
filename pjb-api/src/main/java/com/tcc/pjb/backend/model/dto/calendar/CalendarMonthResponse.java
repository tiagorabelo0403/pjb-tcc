package com.tcc.pjb.backend.model.dto.calendar;

import java.time.YearMonth;
import java.util.List;

public record CalendarMonthResponse(
    YearMonth month,
    List<CalendarDaySummaryDto> days
) {
}
