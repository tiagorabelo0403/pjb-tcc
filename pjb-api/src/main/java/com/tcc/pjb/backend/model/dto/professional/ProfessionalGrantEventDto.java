package com.tcc.pjb.backend.model.dto.professional;

import java.time.LocalDateTime;

public record ProfessionalGrantEventDto(
        Long eventId,
        LocalDateTime createdAt,
        String eventType,
        String eventTypeLabel,
        String previousStatus,
        String newStatus,
        Long actorUserId,
        String actorName,
        String actorClass,
        String detail,
        String tone
) {
}
