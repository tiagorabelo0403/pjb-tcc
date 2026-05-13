package com.tcc.pjb.backend.model.dto.calendar;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

public record CalendarInstitutionalBridgeResponse(
        Instant generatedAt,
        Long usuarioId,
        String profileCode,
        String activeScopeCode,
        String activeInstitutionContextCode,
        String unitLabel,
        Summary summary,
        List<CardDto> cards
) {
    public record Summary(
            int totalCards,
            int totalEvents,
            int criticalEvents,
            int overdueEvents,
            int processCount
    ) {
    }

    public record CardDto(
            String cardKey,
            String unitLabel,
            String contextCode,
            String contextTitle,
            String windowCode,
            String windowLabel,
            String priorityCode,
            String priorityLabel,
            String laneCode,
            String laneTitle,
            String segmentCode,
            String segmentTitle,
            String title,
            String subtitle,
            String color,
            int totalEvents,
            int criticalEvents,
            int overdueEvents,
            int processCount,
            LocalDateTime nextAt,
            String detailsUrl,
            List<String> highlights,
            String presentationCode,
            String presentationTitle,
            String detailCode,
            String detailTitle,
            String iconCode,
            int attentionScore
    ) {
    }
}
