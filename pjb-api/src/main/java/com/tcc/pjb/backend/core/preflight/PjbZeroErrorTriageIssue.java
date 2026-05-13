package com.tcc.pjb.backend.core.preflight;

public record PjbZeroErrorTriageIssue(
        String code,
        String severity,
        String message,
        String suggestedAction
) {
}
