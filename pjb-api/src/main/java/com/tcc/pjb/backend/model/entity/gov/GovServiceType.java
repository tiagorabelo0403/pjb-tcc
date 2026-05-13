package com.tcc.pjb.backend.model.entity.gov;

import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public enum GovServiceType {
    DEEPLINK(
            "DEEPLINK",
            "Deep Link",
            true,
            false,
            Set.of("DEEPLINK", "DEEP_LINK", "LINK", "URL", "WEB", "PORTAL", "REDIRECT", "REDIRECIONAMENTO", "NAVEGACAO")
    ),
    API(
            "API",
            "API",
            false,
            true,
            Set.of("API", "REST", "HTTP", "HTTP_API", "JSON_API", "WEBHOOK", "INTEGRACAO", "INTEGRACAO_API")
    );

    private static final Map<String, GovServiceType> LOOKUP = buildLookup();
    private static final List<String> CANONICAL_VALUES = List.of(DEEPLINK.code, API.code);

    private final String code;
    private final String displayName;
    private final boolean interactive;
    private final boolean machineToMachine;
    private final Set<String> aliases;

    GovServiceType(String code,
                   String displayName,
                   boolean interactive,
                   boolean machineToMachine,
                   Set<String> aliases) {
        this.code = requireText(code, "code");
        this.displayName = requireText(displayName, "displayName");
        this.interactive = interactive;
        this.machineToMachine = machineToMachine;
        this.aliases = normalizeAliases(code, displayName, aliases);
    }

    public String code() {
        return code;
    }

    public String displayName() {
        return displayName;
    }

    public boolean isInteractive() {
        return interactive;
    }

    public boolean isMachineToMachine() {
        return machineToMachine;
    }

    public boolean isDeepLink() {
        return this == DEEPLINK;
    }

    public boolean isApi() {
        return this == API;
    }

    public String externalValue() {
        return code;
    }

    public Set<String> aliases() {
        return aliases;
    }

    public boolean matches(String raw) {
        GovServiceType resolved = tryParse(raw);
        return resolved == this;
    }

    public static GovServiceType defaultValue() {
        return DEEPLINK;
    }

    public static GovServiceType from(String raw) {
        GovServiceType resolved = tryParse(raw);
        if (resolved == null) {
            throw new IllegalArgumentException("Tipo de serviço gov inválido: " + raw);
        }
        return resolved;
    }

    public static GovServiceType tryParse(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return LOOKUP.get(normalize(raw));
    }

    public static GovServiceType coalesce(String raw, GovServiceType fallback) {
        GovServiceType resolved = tryParse(raw);
        if (resolved != null) {
            return resolved;
        }
        return fallback != null ? fallback : defaultValue();
    }

    public static List<String> canonicalValues() {
        return CANONICAL_VALUES;
    }

    private static Map<String, GovServiceType> buildLookup() {
        Map<String, GovServiceType> map = new LinkedHashMap<>();
        for (GovServiceType value : values()) {
            register(map, value.code, value);
            register(map, value.name(), value);
            register(map, value.displayName, value);
            for (String alias : value.aliases) {
                register(map, alias, value);
            }
        }
        return Map.copyOf(map);
    }

    private static void register(Map<String, GovServiceType> map, String raw, GovServiceType value) {
        String normalized = normalize(raw);
        if (normalized == null) {
            return;
        }
        GovServiceType current = map.putIfAbsent(normalized, value);
        if (current != null && current != value) {
            throw new IllegalStateException("Alias duplicado de GovServiceType: " + raw + " -> " + current.name() + " / " + value.name());
        }
    }

    private static Set<String> normalizeAliases(String code, String displayName, Set<String> aliases) {
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        addAlias(normalized, code);
        addAlias(normalized, displayName);
        if (aliases != null) {
            for (String alias : aliases) {
                addAlias(normalized, alias);
            }
        }
        return Set.copyOf(normalized);
    }

    private static void addAlias(Set<String> sink, String raw) {
        String normalized = normalize(raw);
        if (normalized != null) {
            sink.add(normalized);
        }
    }

    private static String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String value = Normalizer.normalize(raw, Normalizer.Form.NFD);
        value = PatternHolder.DIACRITICS.matcher(value).replaceAll("");
        value = value.trim().toUpperCase(Locale.ROOT);
        value = PatternHolder.NON_WORD.matcher(value).replaceAll("_");
        value = collapseUnderscores(value);
        return value.isBlank() ? null : value;
    }

    private static String collapseUnderscores(String raw) {
        StringBuilder out = new StringBuilder(raw.length());
        boolean lastUnderscore = false;
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            boolean underscore = c == '_';
            if (underscore) {
                if (!lastUnderscore) {
                    out.append(c);
                }
            } else {
                out.append(c);
            }
            lastUnderscore = underscore;
        }
        int start = 0;
        int end = out.length();
        while (start < end && out.charAt(start) == '_') {
            start++;
        }
        while (end > start && out.charAt(end - 1) == '_') {
            end--;
        }
        return out.substring(start, end);
    }

    private static final class PatternHolder {
        private static final Pattern DIACRITICS = Pattern.compile("\\p{M}+");
        private static final Pattern NON_WORD = Pattern.compile("[^A-Z0-9]+");

        private PatternHolder() {
        }
    }

    private static String requireText(String value, String field) {
        String normalized = value == null ? null : value.trim();
        if (normalized == null || normalized.isEmpty()) {
            throw new IllegalArgumentException(field + " obrigatório");
        }
        return normalized;
    }
}
