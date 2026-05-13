package com.tcc.pjb.backend.model.dto.calendar;

import java.time.Instant;
import java.util.List;

public record UserCalendarPreferenceResponse(
        Long usuarioId,
        List<String> visibleLaneCodes,
        List<String> pinnedLaneCodes,
        List<String> hiddenLaneCodes,
        String defaultView,
        boolean includePersonalCalendar,
        boolean includeInstitutionalCalendar,
        boolean highlightUrgentDays,
        String selectedScopeCode,
        Long selectedTeamId,
        String selectedInstitutionContextCode,
        String notificationCadenceMode,
        List<String> notificationLaneCodes,
        List<ScopeOptionDto> availableScopes,
        List<InstitutionalContextOptionDto> availableInstitutionContexts,
        Instant updatedAt
) {
    public record ScopeOptionDto(
            String scopeCode,
            String scopeTitle,
            String institutionLabel,
            String scopeKind,
            boolean active
    ) {
    }

    public record InstitutionalContextOptionDto(
            String contextCode,
            String contextTitle,
            String contextLabel,
            String contextKind,
            boolean active
    ) {
    }
}

