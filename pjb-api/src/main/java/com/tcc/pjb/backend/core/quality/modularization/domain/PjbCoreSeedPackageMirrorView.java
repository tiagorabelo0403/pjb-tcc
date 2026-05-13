package com.tcc.pjb.backend.core.quality.modularization.domain;

import java.util.List;
import java.util.Objects;

public record PjbCoreSeedPackageMirrorView(
        String packageName,
        String sourceRoot,
        String moduleRoot,
        List<String> sourceFiles,
        List<String> moduleFiles
) {

    public PjbCoreSeedPackageMirrorView {
        packageName = requireText(packageName, "packageName");
        sourceRoot = requireText(sourceRoot, "sourceRoot");
        moduleRoot = requireText(moduleRoot, "moduleRoot");
        sourceFiles = List.copyOf(Objects.requireNonNull(sourceFiles));
        moduleFiles = List.copyOf(Objects.requireNonNull(moduleFiles));
    }

    private static String requireText(String value, String field) {
        String normalized = Objects.requireNonNull(value, field).trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return normalized;
    }
}
