package com.tcc.pjb.backend.integration.oab;

import java.time.Instant;

public record OabValidationResult(
        OabValidationStatus status,
        String reasonCode,
        String source,
        Instant checkedAt
) {
    public OabValidationResult {
        status = status == null ? OabValidationStatus.INDETERMINADO : status;
        reasonCode = normalize(reasonCode, "UNKNOWN");
        source = normalize(source, "oab-cna");
        checkedAt = checkedAt == null ? Instant.now() : checkedAt;
    }

    public static OabValidationResult apto(String source) {
        return new OabValidationResult(OabValidationStatus.APTO, "OAB_APTA", source, Instant.now());
    }

    public static OabValidationResult inapto(String reasonCode, String source) {
        return new OabValidationResult(OabValidationStatus.INAPTO, reasonCode, source, Instant.now());
    }

    public static OabValidationResult indeterminado(String reasonCode, String source) {
        return new OabValidationResult(OabValidationStatus.INDETERMINADO, reasonCode, source, Instant.now());
    }

    private static String normalize(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }
}
