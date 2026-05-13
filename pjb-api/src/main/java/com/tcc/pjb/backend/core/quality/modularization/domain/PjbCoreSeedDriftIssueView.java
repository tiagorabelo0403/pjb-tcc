package com.tcc.pjb.backend.core.quality.modularization.domain;

import java.util.Objects;

public record PjbCoreSeedDriftIssueView(
        String severity,
        String fileName,
        String issueType,
        String summary
) {

    public PjbCoreSeedDriftIssueView {
        severity = requireText(severity, "severity");
        fileName = requireText(fileName, "fileName");
        issueType = requireText(issueType, "issueType");
        summary = requireText(summary, "summary");
    }

    private static String requireText(String value, String field) {
        String normalized = Objects.requireNonNull(value, field).trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return normalized;
    }
}
