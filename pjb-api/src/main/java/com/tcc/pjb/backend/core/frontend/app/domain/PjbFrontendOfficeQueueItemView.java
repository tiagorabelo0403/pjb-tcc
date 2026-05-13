package com.tcc.pjb.backend.core.frontend.app.domain;

import java.time.LocalDateTime;

public record PjbFrontendOfficeQueueItemView(
        Long queueItemId,
        Long equipeId,
        Long executorUserId,
        Long signerUserId,
        String actionType,
        String resourceType,
        String resourceId,
        String status,
        LocalDateTime createdAt,
        LocalDateTime decidedAt,
        Long decidedByUserId,
        String decisionReason,
        String requestId,
        String payloadHash,
        String summary
) {
}
