package com.tcc.pjb.backend.model.dto.admin;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record RitoFeedbackResponse(
        UUID feedbackId,
        Long processoId,
        String ritoResolved,
        String ritoChosen,
        Double confidence,
        boolean overrideApplied,
        OffsetDateTime createdAt,
        List<String> reasons
) {
}
