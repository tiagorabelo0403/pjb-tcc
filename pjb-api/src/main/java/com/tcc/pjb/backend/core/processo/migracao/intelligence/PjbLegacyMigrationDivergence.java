package com.tcc.pjb.backend.core.processo.migracao.intelligence;

import java.util.Objects;

public record PjbLegacyMigrationDivergence(
        PjbLegacyMigrationDivergenceType type,
        String severity,
        String legacyReference,
        String normalizedReference,
        boolean blocking
) {
    public PjbLegacyMigrationDivergence {
        type = type == null ? PjbLegacyMigrationDivergenceType.HISTORICAL_EVENT_LOSS : type;
        severity = normalize(severity);
        legacyReference = Objects.toString(legacyReference, "").trim();
        normalizedReference = Objects.toString(normalizedReference, "").trim();
        blocking = blocking || "CRITICAL".equals(severity);
    }

    private static String normalize(String value) {
        String normalized = Objects.toString(value, "MEDIUM").trim().toUpperCase();
        return switch (normalized) {
            case "LOW", "MEDIUM", "HIGH", "CRITICAL" -> normalized;
            default -> "MEDIUM";
        };
    }
}
