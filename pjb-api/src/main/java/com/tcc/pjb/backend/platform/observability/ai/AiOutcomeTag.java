package com.tcc.pjb.backend.platform.observability.ai;

import java.util.Locale;
import org.springframework.http.HttpStatus;

public final class AiOutcomeTag {

    private AiOutcomeTag() {}

    public static String ofHttpStatus(HttpStatus status) {
        if (status == null) return "500_ERROR";
        return status.value() + "_" + status.name();
    }

    public static String ofStatusCode(int statusCode) {
        try {
            return ofHttpStatus(HttpStatus.valueOf(statusCode));
        } catch (Exception e) {
            return statusCode + "_UNKNOWN";
        }
    }

    public static String normalize(String raw) {
        if (raw == null || raw.isBlank()) return "500_ERROR";
        String s = raw.trim().toUpperCase(Locale.ROOT);
        s = s.replaceAll("[^A-Z0-9_]+", "_");
        if (s.length() > 40) s = s.substring(0, 40);
        return s;
    }
}
