package com.tcc.pjb.backend.model.dto.calendar;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

public record CalendarInstitutionalFocusResponse(
        Instant generatedAt,
        String profileCode,
        String activeScopeCode,
        String activeInstitutionContextCode,
        List<FocusSliceDto> focusSlices,
        List<WindowBucketDto> windows,
        List<PriorityBucketDto> priorities,
        List<MilestoneDto> milestones
) {
    public record FocusSliceDto(
            String sliceCode,
            String sliceTitle,
            String color,
            int totalCards,
            int totalEvents,
            int criticalEvents,
            int overdueEvents,
            int processCount,
            LocalDateTime nextAt,
            List<String> highlights,
            List<FocusDetailDto> detailBuckets,
            String presentationCode,
            String iconCode,
            int attentionScore
    ) {
    }

    public record FocusDetailDto(
            String detailCode,
            String detailTitle,
            String color,
            int totalCards,
            int totalEvents,
            int criticalEvents,
            int overdueEvents,
            int processCount,
            LocalDateTime nextAt,
            List<String> highlights,
            String presentationCode,
            String iconCode,
            int attentionScore
    ) {
    }

    public record WindowBucketDto(
            String windowCode,
            String windowLabel,
            int totalCards,
            int totalEvents,
            int criticalEvents,
            int overdueEvents
    ) {
    }

    public record PriorityBucketDto(
            String priorityCode,
            String priorityLabel,
            int totalCards,
            int totalEvents
    ) {
    }

    public record MilestoneDto(
            String cardKey,
            String title,
            String subtitle,
            LocalDateTime nextAt,
            String color,
            String detailsUrl,
            String presentationCode,
            String iconCode,
            int attentionScore
    ) {
    }
}
