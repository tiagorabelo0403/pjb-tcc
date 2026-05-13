package com.tcc.pjb.backend.core.correlation;

import java.util.Objects;
import java.util.UUID;

public record CausationId(String value) {

    public CausationId {
        value = normalize(value, "value");
    }

    public static CausationId random() {
        return new CausationId(UUID.randomUUID().toString());
    }

    public static CausationId of(String value) {
        return new CausationId(value);
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
