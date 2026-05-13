package com.tcc.pjb.backend.model.dto.admin;

import java.time.OffsetDateTime;

public record RitoMostCorrectedProcessDto(
        Long processoId,
        String numeroUnificado,
        long feedbackCount,
        OffsetDateTime lastFeedbackAt
) {
    public long corrections() { return feedbackCount(); }
    public java.time.OffsetDateTime lastCorrectionAt() { return lastFeedbackAt(); }
}

