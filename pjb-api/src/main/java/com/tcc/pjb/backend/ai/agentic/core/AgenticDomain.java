package com.tcc.pjb.backend.ai.agentic.core;

import java.util.Locale;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum AgenticDomain {
    LEGAL,
    FINANCE;

    @JsonCreator
    public static AgenticDomain fromJson(String raw) {
        if (raw == null || raw.isBlank()) {
            return LEGAL;
        }
        String v = raw.trim().toLowerCase(Locale.ROOT);
        if (v.contains("health") || v.contains("medical") || v.contains("saude") || v.contains("saúde")) {
            return LEGAL;
        }
        if (v.contains("finance") || v.contains("finan") || v.contains("contab") || v.contains("contáb")) {
            return FINANCE;
        }
        if ("legal".equals(v) || "juridico".equals(v) || "jurídico".equals(v) || "juridica".equals(v) || "jurídica".equals(v)) {
            return LEGAL;
        }
        try {
            return AgenticDomain.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            return LEGAL;
        }
    }

    @JsonValue
    public String toJson() {
        return name();
    }
}
