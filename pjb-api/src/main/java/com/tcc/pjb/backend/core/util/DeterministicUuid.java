package com.tcc.pjb.backend.core.util;

import java.nio.charset.StandardCharsets;
import java.util.StringJoiner;
import java.util.UUID;

public final class DeterministicUuid {

    private DeterministicUuid() {
    }

    public static UUID v5(String namespace, String value) {
        return UUID.nameUUIDFromBytes(seed(namespace, value).getBytes(StandardCharsets.UTF_8));
    }

    public static UUID v5(String namespace, String value, String... extraValues) {
        return UUID.nameUUIDFromBytes(seed(namespace, value, extraValues).getBytes(StandardCharsets.UTF_8));
    }

    public static String seed(String namespace, String value, String... extraValues) {
        StringJoiner joiner = new StringJoiner("#");
        joiner.add(normalize(namespace));
        joiner.add(normalize(value));
        if (extraValues != null) {
            for (String item : extraValues) {
                joiner.add(normalize(item));
            }
        }
        return joiner.toString();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
