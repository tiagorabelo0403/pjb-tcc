package com.tcc.pjb.backend.platform.versioning;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class VersionHints {

    private VersionHints() {}

    public static ApiVersion resolveVersion(Map<String, Object> payload, ApiVersion defaultVersion) {
        Objects.requireNonNull(defaultVersion, "defaultVersion");
        if (payload == null || payload.isEmpty()) return defaultVersion;

        String raw = firstString(payload,
                "apiVersion", "api_version",
                "iaVersion", "ia_version",
                "version", "versao", "versão",
                "contractVersion", "contract_version"
        );

        if (raw != null && !raw.isBlank()) {
            return ApiVersion.parseLenient(raw, defaultVersion);
        }

        
        String token = firstString(payload, "capability", "skill", "acao", "action", "tipo");
        return ApiVersion.inferFromToken(token).orElse(defaultVersion);
    }

    public static String resolveCapability(Map<String, Object> payload, String fallback) {
        String cap = payload != null ? firstString(payload, "capability", "skill", "acao", "action") : null;
        if (cap != null && !cap.isBlank()) return cap;
        return fallback;
    }

    private static String firstString(Map<String, Object> payload, String... keys) {
        for (String k : keys) {
            Object v = payload.get(k);
            if (v == null) continue;
            String s = Objects.toString(v, null);
            if (s != null && !s.isBlank()) return s.trim();
        }
        
        for (String k : keys) {
            String target = k.toLowerCase(Locale.ROOT);
            Optional<String> found = payload.entrySet().stream()
                    .filter(e -> e.getKey() != null && e.getKey().toLowerCase(Locale.ROOT).equals(target))
                    .map(e -> Objects.toString(e.getValue(), null))
                    .filter(s -> s != null && !s.isBlank())
                    .map(String::trim)
                    .findFirst();
            if (found.isPresent()) return found.get();
        }
        return null;
    }
}
