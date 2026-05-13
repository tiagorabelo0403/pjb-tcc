package com.tcc.pjb.backend.core.frontend.app.domain;

import java.util.List;

public record PjbFrontendOfficeWorkspaceProcessCardView(
        Long processoId,
        String numeroProcesso,
        String ramoDireito,
        String statusProcesso,
        String nivelSigilo,
        String accentColor,
        String statusColor,
        String ramoColor,
        String sigiloColor,
        boolean visibleInWorkspace,
        boolean sensitive,
        boolean ownPersonalCase,
        boolean patronSigningContext,
        List<String> blockers,
        List<String> warnings,
        String readingModeRoute,
        String timelineRoute,
        String calendarRoute,
        String calculatorRoute,
        String prazoRealRoute
) {
}
