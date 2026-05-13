package com.tcc.pjb.backend.core.frontend.app.domain;

import com.tcc.pjb.backend.model.dto.calendar.CalendarWorkspaceResponse;
import com.tcc.pjb.backend.model.dto.processual.calculo.CalculoJudicialWorkspaceResponse;
import java.util.List;

public record PjbFrontendOfficeWorkspaceLegalCockpitView(
        String officeMode,
        Long activeEquipeId,
        String activeEquipeNome,
        PjbFrontendOfficeWorkspaceSummaryView officeSummary,
        PjbFrontendOfficeWorkspaceProcessPageView processPage,
        List<PjbFrontendOfficeWorkspaceProcessCardView> highlightedProcesses,
        CalendarWorkspaceResponse deadlineCalendar,
        CalculoJudicialWorkspaceResponse calculatorWorkspace,
        PjbFrontendOfficeProcessReadingModeView selectedProcessReading,
        List<String> linkedModules,
        List<String> quickRoutes,
        List<String> blockers,
        List<String> warnings
) {
}
