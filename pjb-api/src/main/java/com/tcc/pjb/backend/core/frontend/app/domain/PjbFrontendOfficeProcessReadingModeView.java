package com.tcc.pjb.backend.core.frontend.app.domain;

import com.tcc.pjb.backend.model.dto.calendar.CalendarWorkspaceResponse;
import com.tcc.pjb.backend.model.dto.timeline.TimelineItemResponse;
import java.util.List;

public record PjbFrontendOfficeProcessReadingModeView(
        Long processoId,
        String numeroProcesso,
        String officeMode,
        Long activeEquipeId,
        String activeEquipeNome,
        boolean readOnly,
        String accentColor,
        String statusColor,
        String ramoColor,
        String sigiloColor,
        PjbFrontendOfficeProcessAccessView access,
        CalendarWorkspaceResponse deadlineCalendar,
        List<TimelineItemResponse> timeline,
        List<String> linkedFeatures,
        List<String> availableRoutes,
        List<String> blockers,
        List<String> warnings
) {
}
