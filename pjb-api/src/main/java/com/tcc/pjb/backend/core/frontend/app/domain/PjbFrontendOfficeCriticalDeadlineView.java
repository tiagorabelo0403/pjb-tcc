package com.tcc.pjb.backend.core.frontend.app.domain;

import java.time.Instant;

public record PjbFrontendOfficeCriticalDeadlineView(
        Long workItemId,
        Long processoId,
        String numeroProcesso,
        String titulo,
        Instant dueAt,
        long horasRestantes,
        String accentColor,
        String statusColor,
        String readingModeRoute,
        String calendarRoute
) {
}
