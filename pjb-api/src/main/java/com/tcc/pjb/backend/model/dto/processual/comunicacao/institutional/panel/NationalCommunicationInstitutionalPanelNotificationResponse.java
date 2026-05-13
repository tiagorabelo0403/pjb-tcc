package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.panel;

import java.time.Instant;

public record NationalCommunicationInstitutionalPanelNotificationResponse(
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
