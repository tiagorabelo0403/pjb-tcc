package com.tcc.pjb.backend.model.dto.consultapublica;

import java.time.LocalDate;
import java.util.List;

public record ConsultaPublicaPersonalCalendarDigestDto(
        LocalDate from,
        LocalDate to,
        int totalEvents,
        int highlightedEvents,
        int criticalDays,
        String dominantLane,
        String dominantColor,
        List<String> focusLabels
) {
}
