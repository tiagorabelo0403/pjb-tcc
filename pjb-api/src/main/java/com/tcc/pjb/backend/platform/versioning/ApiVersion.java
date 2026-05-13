package com.tcc.pjb.backend.platform.versioning;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

public enum ApiVersion {

    V1(1, "v1"),
    V2(2, "v2"),
    V3(3, "v3");

    private final int major;
    private final String canonical;

    ApiVersion(int major, String canonical) {
        this.major = major;
        this.canonical = canonical;
    }

    public int major() {
        return major;
    }

    public String canonical() {
        return canonical;
    }

    public static ApiVersion latest() {
        return V3;
    }

    
    public boolean isAtLeast(ApiVersion other) {
        if (other == null) return true;
        return this.major >= other.major;
    }

    public Optional<ApiVersion> previous() {
        return switch (this) {
            case V3 -> Optional.of(V2);
            case V2 -> Optional.of(V1);
            case V1 -> Optional.empty();
        };
    }

    public static ApiVersion parseLenient(String raw, ApiVersion defaultValue) {
        if (raw == null || raw.isBlank()) return Objects.requireNonNull(defaultValue, "defaultValue");
        return tryParse(raw).orElse(defaultValue);
    }

    public static Optional<ApiVersion> tryParse(String raw) {
        if (raw == null) return Optional.empty();
        String s = raw.trim().toLowerCase(Locale.ROOT);
        if (s.isBlank()) return Optional.empty();

        
        if (s.startsWith("version")) {
            int idx = s.indexOf(':');
            if (idx > 0 && idx < s.length() - 1) {
                s = s.substring(idx + 1).trim();
            }
        }
        if (s.startsWith("v")) s = s.substring(1).trim();
        int dot = s.indexOf('.');
        if (dot > 0) s = s.substring(0, dot);

        return switch (s) {
            case "1" -> Optional.of(V1);
            case "2" -> Optional.of(V2);
            case "3" -> Optional.of(V3);
            default -> Optional.empty();
        };
    }

    public static Optional<ApiVersion> inferFromToken(String token) {
        if (token == null || token.isBlank()) return Optional.empty();
        String s = token.toUpperCase(Locale.ROOT);
        if (s.contains("_V3") || s.contains("/V3") || s.contains("-V3")) return Optional.of(V3);
        if (s.contains("_V2") || s.contains("/V2") || s.contains("-V2")) return Optional.of(V2);
        if (s.contains("_V1") || s.contains("/V1") || s.contains("-V1")) return Optional.of(V1);
        return Optional.empty();
    }
}
