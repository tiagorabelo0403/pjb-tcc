package com.tcc.pjb.backend.model.dto.governance;

import java.math.BigDecimal;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DecisionTrailSnapshotRequest(
        @NotNull Long processoId,
        @NotBlank String decisionType,
        BigDecimal confidence,
        String reasonsJson,
        String citationsJson,
        String inputDigest,
        String outputDigest,
        String modelVersion,
        String metadataJson
) {
}
