package com.tcc.pjb.backend.model.dto.calendar;

import java.util.List;

public record UserCalendarPreferenceRequest(
        List<String> visibleLaneCodes,
        List<String> pinnedLaneCodes,
        List<String> hiddenLaneCodes,
        String defaultView,
        Boolean includePersonalCalendar,
        Boolean includeInstitutionalCalendar,
        Boolean highlightUrgentDays,
        String selectedScopeCode,
        Long selectedTeamId,
        String selectedInstitutionContextCode,
        String notificationCadenceMode,
        List<String> notificationLaneCodes
) {
}
