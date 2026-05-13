package com.tcc.pjb.backend.model.dto.ui;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import com.tcc.pjb.backend.model.entity.ui.UiSubjectType;
import lombok.Builder;

@Builder
public record UiHistoryEntryDto(
    UUID id,
    UiSubjectType subjectType,
    Long processoId,
    Long workItemId,
    String inboxKey,
    String eventType,
    String fromStatus,
    String toStatus,
    List<UiToken> fromTokens,
    List<UiToken> toTokens,
    Long actorUserId,
    String actorRole,
    String message,
    Instant occurredAt
) {
}
