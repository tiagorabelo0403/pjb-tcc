package com.tcc.pjb.backend.platform.observability.ai;

import java.util.Locale;

public enum AiTelemetryDomain {
    LEGAL("legal"),
    FINANCE("finance");

    private final String tag;

    AiTelemetryDomain(String tag) {
        this.tag = tag;
    }

    public String tag() {
        return tag;
    }

    public static AiTelemetryDomain parse(String raw, AiTelemetryDomain def) {
        if (raw == null || raw.isBlank()) return def;
        String s = raw.trim().toLowerCase(Locale.ROOT);
        return switch (s) {
            case "legal", "juridico", "jurídico", "juridica", "jurídica" -> LEGAL;
            case "finance", "financial", "financeiro", "financeira" -> FINANCE;
            default -> def;
        };
    }
}
