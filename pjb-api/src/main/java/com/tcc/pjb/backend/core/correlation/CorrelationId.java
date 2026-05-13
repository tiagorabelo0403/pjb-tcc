package com.tcc.pjb.backend.core.correlation;

import java.util.Objects;
import java.util.UUID;

public record CorrelationId(String value) {

    public CorrelationId {
        value = normalize(value, "value");
    }

    public static CorrelationId random() {
        return new CorrelationId(UUID.randomUUID().toString());
    }

    public static CorrelationId of(String value) {
        return new CorrelationId(value);
    }

    public String headerValue() {
        return value;
    }

    private static String normalize(String value, String field) {
        String normalized = Objects.requireNonNull(value, field).trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return normalized;
    }
}
