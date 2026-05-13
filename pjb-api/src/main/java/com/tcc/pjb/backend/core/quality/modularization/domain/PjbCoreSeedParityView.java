package com.tcc.pjb.backend.core.quality.modularization.domain;

import java.util.Objects;

public record PjbCoreSeedParityView(
        String className,
        boolean sourcePresent,
        boolean modulePresent,
        boolean contentAligned,
        String sourceHash,
        String moduleHash
) {

    public PjbCoreSeedParityView {
        className = requireText(className, "className");
        sourceHash = normalize(sourceHash);
        moduleHash = normalize(moduleHash);
    }

    private static String requireText(String value, String field) {
        String normalized = Objects.requireNonNull(value, field).trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return normalized;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
