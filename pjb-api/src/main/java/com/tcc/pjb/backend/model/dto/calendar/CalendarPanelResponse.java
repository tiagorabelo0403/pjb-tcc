package com.tcc.pjb.backend.model.dto.calendar;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record CalendarPanelResponse(
        Instant generatedAt,
        LocalDate from,
        LocalDate to,
        PanelProfileDto profile,
        List<PanelScopeDto> scopes,
        List<PanelInstitutionalContextDto> institutionalContexts,
        List<PanelDayCellDto> dayCells,
        List<HighlightedEventDto> highlightedEvents,
        List<ProcessSpotlightDto> processSpotlights,
        List<CalendarWorkspaceResponse.CalendarColorLegendDto> colorLegend,
        CalendarNotificationPreviewResponse notificationPreview,
        CalendarInstitutionalBridgeResponse institutionalBridge,
        CalendarInstitutionalFocusResponse institutionalFocus
) {
    public record PanelProfileDto(
            String profileCode,
            String profileTitle,
            String defaultView,
            String highlightLaneCode,
            boolean includePersonalCalendar,
            boolean includeInstitutionalCalendar,
            boolean highlightUrgentDays,
            String activeScopeCode,
            String activeInstitutionContextCode,
            String notificationCadenceMode
    ) {
    }

    public record PanelScopeDto(
            String scopeCode,
            String scopeTitle,
            String institutionLabel,
            boolean active
    ) {
    }

    public record PanelInstitutionalContextDto(
            String contextCode,
            String contextTitle,
            String contextLabel,
            String contextKind,
            boolean active
    ) {
    }

    public record PanelDayCellDto(
            LocalDate day,
            String dominantColor,
            int totalEvents,
            int criticalEvents,
            int attentionScore,
            List<String> laneCodes,
            List<String> detailCodes,
            List<String> labels
    ) {
    }

    public record HighlightedEventDto(
            Long processoId,
            String processoNumero,
            String laneCode,
            String segmentCode,
            String presentationCode,
            String presentationTitle,
            String iconCode,
            int attentionScore,
            String title,
            String subtitle,
            @Schema(description = "Data e hora do evento destacado no calendário", format = "date-time",
                    example = "2026-06-01T14:00:00-03:00") LocalDateTime at,
            String color,
            String detailsUrl,
            String audienceCode
    ) {
    }

    public record ProcessSpotlightDto(
            Long processoId,
            String processoNumero,
            String presentationCode,
            String presentationTitle,
            String title,
            String color,
            int attentionScore,
            LocalDateTime at,
            String detailsUrl
    ) {
    }
}
