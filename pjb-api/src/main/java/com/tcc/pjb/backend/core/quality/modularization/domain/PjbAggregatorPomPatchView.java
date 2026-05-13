package com.tcc.pjb.backend.core.quality.modularization.domain;

import java.time.Instant;
import java.util.List;

public record PjbAggregatorPomPatchView(
        String targetFile,
        boolean generatedFilePresent,
        List<String> modules,
        List<String> patchLines,
        Instant generatedAt) {
}
