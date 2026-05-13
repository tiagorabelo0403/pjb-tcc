package com.tcc.pjb.backend.core.comunicacao.institucional.panel.domain;

import java.time.Instant;

public record InstitutionalPanelNotification(
        String notificationId,
        String severity,
        String title,
        String message,
        String accentColor,
        String actionLabel,
        String actionPath,
        Instant createdAt
) {
}
