package com.tcc.pjb.backend.model.dto.calendar;

import java.time.LocalDate;
import java.util.List;

public record CalendarWorkspaceResponse(
        LocalDate from,
        LocalDate to,
        CalendarProfileDto profile,
        List<CalendarColorLegendDto> colorLegend,
        List<CalendarLaneDto> lanes
) {
    public record CalendarProfileDto(
            String profileCode,
            String profileTitle,
            String highlightLaneCode,
            List<String> visibleLaneCodes,
            List<String> pinnedLaneCodes,
            List<CalendarPrazoTrackDto> prazoTracks,
            boolean personalEventsEnabled
    ) {
    }

    public record CalendarPrazoTrackDto(
            String trackCode,
            String trackTitle,
            String regime,
            String summary,
            boolean highlighted
    ) {
    }

    public record CalendarColorLegendDto(
            String colorCode,
            String label,
            String meaning
    ) {
    }

    public record CalendarLaneDto(
            String laneCode,
            String laneTitle,
            String accentColor,
            boolean visible,
            boolean pinned,
            int totalEvents,
            List<CalendarSegmentDto> segments,
            List<CalendarDayDto> days
    ) {
    }

    public record CalendarSegmentDto(
            String segmentCode,
            String segmentTitle,
            int totalEvents
    ) {
    }

    public record CalendarDayDto(
            LocalDate day,
            List<CalendarWorkspaceEventDto> events
    ) {
    }
}
