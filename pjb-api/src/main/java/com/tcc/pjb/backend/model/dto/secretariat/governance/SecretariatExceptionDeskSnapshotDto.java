package com.tcc.pjb.backend.model.dto.secretariat.governance;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record SecretariatExceptionDeskSnapshotDto(
        Instant generatedAt,
        String inboxKey,
        String inboxDescriptor,
        Map<String, Object> metrics,
        List<ExceptionCase> exceptions,
        List<String> warnings,
        Map<String, Object> routes
) {
    public record ExceptionCase(
            Long workItemId,
            Long processoId,
            String titulo,
            String status,
            String severity,
            String reasonCode,
            String reasonLabel,
            Instant dueAt,
            boolean blocking,
            List<String> requiredActions,
            Map<String, Object> metadata
    ) {
    }
}
