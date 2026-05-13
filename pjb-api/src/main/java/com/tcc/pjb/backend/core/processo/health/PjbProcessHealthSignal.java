package com.tcc.pjb.backend.core.processo.health;

import java.util.Objects;

public record PjbProcessHealthSignal(
        PjbProcessHealthSignalType type,
        String severity,
        boolean blocking,
        String message,
        String evidence
) {
    public PjbProcessHealthSignal {
        type = type == null ? PjbProcessHealthSignalType.QUEUE_BACKLOG : type;
        severity = normalizeSeverity(severity);
        message = Objects.toString(message, "").trim();
        evidence = Objects.toString(evidence, "").trim();
    }

    private static String normalizeSeverity(String value) {
        String normalized = Objects.toString(value, "MEDIUM").trim().toUpperCase();
        return switch (normalized) {
            case "LOW", "MEDIUM", "HIGH", "CRITICAL" -> normalized;
            default -> "MEDIUM";
        };
    }
}
