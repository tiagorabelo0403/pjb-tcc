package com.tcc.pjb.backend.core.frontend.publicaccess;

import java.time.Instant;
import java.util.Objects;

public record PjbPublicTimelineEntry(
        String movementCode,
        String technicalLabel,
        String plainLanguageLabel,
        String nextStepHint,
        Instant occurredAt,
        boolean visibleToPublic
) {
    public PjbPublicTimelineEntry {
        movementCode = Objects.toString(movementCode, "").trim();
        technicalLabel = Objects.toString(technicalLabel, "").trim();
        plainLanguageLabel = Objects.toString(plainLanguageLabel, "").trim();
        nextStepHint = Objects.toString(nextStepHint, "").trim();
        occurredAt = occurredAt == null ? Instant.EPOCH : occurredAt;
    }
}
