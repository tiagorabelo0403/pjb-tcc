package com.tcc.pjb.backend.model.dto.secretariat.queue;

import java.time.Instant;
import java.util.Map;

public record SecretariatQueueOperationalActionResponse(
    Long workItemId,
    Long processoId,
    String inboxKey,
    String actionCode,
    String queueStatus,
    String confirmationStatus,
    String venueConfirmationStatus,
    String participantNotificationStatus,
    String attendanceStatus,
    String completionEventStatus,
    String processReturnStatus,
    boolean autoReturnReady,
    Instant updatedAt,
    String panelRoute,
    String agendaRoute,
    String targetPanelRoute,
    Long reentryWorkItemId,
    Map<String, Object> checkpoint,
    Map<String, Object> metadata,
    Map<String, Object> generatedDocument
) {
}
