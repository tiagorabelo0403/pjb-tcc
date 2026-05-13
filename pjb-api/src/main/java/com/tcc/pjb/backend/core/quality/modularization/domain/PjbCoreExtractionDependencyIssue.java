package com.tcc.pjb.backend.core.quality.modularization.domain;

import java.util.List;

public record PjbCoreExtractionDependencyIssue(
        String severity,
        String sourceFile,
        String dependencyType,
        String summary,
        List<String> actions
) {
    public PjbCoreExtractionDependencyIssue {
        severity = severity == null ? "MEDIO" : severity;
        sourceFile = sourceFile == null ? "" : sourceFile;
        dependencyType = dependencyType == null ? "unknown" : dependencyType;
        summary = summary == null ? "" : summary;
        actions = actions == null ? List.of() : List.copyOf(actions);
    }
}
