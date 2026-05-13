package com.tcc.pjb.backend.core.quality.modularization.domain;

import java.time.Instant;
import java.util.List;

public record PjbCoreExtractionPomPreview(
        boolean aggregatorPomRequired,
        List<String> suggestedModules,
        List<String> previewLines,
        Instant generatedAt
) {
    public PjbCoreExtractionPomPreview {
        suggestedModules = suggestedModules == null ? List.of() : List.copyOf(suggestedModules);
        previewLines = previewLines == null ? List.of() : List.copyOf(previewLines);
        generatedAt = generatedAt == null ? Instant.now() : generatedAt;
    }
}
