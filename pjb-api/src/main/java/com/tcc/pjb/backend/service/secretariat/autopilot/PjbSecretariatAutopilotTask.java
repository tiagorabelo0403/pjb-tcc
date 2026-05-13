package com.tcc.pjb.backend.service.secretariat.autopilot;

import java.time.Instant;
import java.util.Objects;

public record PjbSecretariatAutopilotTask(
        String processNumber,
        PjbSecretariatAutopilotTaskType type,
        Instant dueAt,
        int urgency,
        int queueAgeDays,
        boolean legallyPreferred,
        String reason
) {
    public PjbSecretariatAutopilotTask {
        processNumber = Objects.toString(processNumber, "").trim();
        type = type == null ? PjbSecretariatAutopilotTaskType.QUEUE_BACKLOG : type;
        dueAt = dueAt == null ? Instant.EPOCH : dueAt;
        urgency = Math.max(0, Math.min(100, urgency));
        queueAgeDays = Math.max(0, queueAgeDays);
        reason = Objects.toString(reason, "").trim();
    }
}
