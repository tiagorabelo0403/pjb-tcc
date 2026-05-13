package com.tcc.pjb.backend.core.kernel.recursal;

import java.util.Map;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.tcc.pjb.backend.core.util.EnumText;

public enum LegalIntegrationSystem {

    PJE,
    EPROC,
    ESAJ,
    PROJUDI,
    DIARIO_OFICIAL,
    MANUAL,
    OTHER;

    private static final Map<String, LegalIntegrationSystem> ALIASES = Map.ofEntries(
            Map.entry("PJE", PJE),
            Map.entry("PJE2", PJE),
            Map.entry("EPROC", EPROC),
            Map.entry("E-PROC", EPROC),
            Map.entry("ESAJ", ESAJ),
            Map.entry("E-SAJ", ESAJ),
            Map.entry("SAJ", ESAJ),
            Map.entry("PROJUDI", PROJUDI),
            Map.entry("DIARIO", DIARIO_OFICIAL),
            Map.entry("DIARIO_OFICIAL", DIARIO_OFICIAL),
            Map.entry("MANUAL", MANUAL)
    );

    public static LegalIntegrationSystem fromString(String raw) {
        if (raw == null || raw.isBlank()) return OTHER;
        String token = EnumText.normalizeToken(raw);
        if (token.isBlank()) return OTHER;
        LegalIntegrationSystem alias = ALIASES.get(token);
        if (alias != null) return alias;
        try {
            return LegalIntegrationSystem.valueOf(token);
        } catch (Exception ignored) {
            return OTHER;
        }
    }

    @JsonCreator
    public static LegalIntegrationSystem jsonCreator(String raw) {
        return fromString(raw);
    }
}
