package com.tcc.pjb.backend.ai.core.enums;

import java.util.Map;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.tcc.pjb.backend.core.util.EnumText;

public enum IADomain {

    JURIDICO_GERAL,
    PENAL,
    POLICIAL,
    FINANCEIRO,
    TRIBUTARIO,
    ADMINISTRATIVO,
    PSICOLOGICO;

    private static final Map<String, IADomain> ALIASES = Map.ofEntries(
            Map.entry("GERAL", JURIDICO_GERAL),
            Map.entry("JURIDICA", JURIDICO_GERAL),
            Map.entry("JURIDICO", JURIDICO_GERAL),
            Map.entry("DIREITO", JURIDICO_GERAL),
            Map.entry("CIVIL", JURIDICO_GERAL),
            Map.entry("CRIMINAL", PENAL),
            Map.entry("SEGURANCA_PUBLICA", POLICIAL),
            Map.entry("POLICIA", POLICIAL),
            Map.entry("FINANCAS", FINANCEIRO),
            Map.entry("FISCAL", TRIBUTARIO),
            Map.entry("TRIBUTOS", TRIBUTARIO),
            Map.entry("ADM", ADMINISTRATIVO),
            Map.entry("PSICO", PSICOLOGICO)
    );

    
    public static IADomain fromString(String raw) {
        if (raw == null || raw.isBlank()) return JURIDICO_GERAL;
        String token = EnumText.normalizeToken(raw);
        if (token.isBlank()) return JURIDICO_GERAL;
        IADomain alias = ALIASES.get(token);
        if (alias != null) return alias;
        try {
            return IADomain.valueOf(token);
        } catch (Exception ignored) {
            return JURIDICO_GERAL;
        }
    }

    @JsonCreator
    public static IADomain jsonCreator(String raw) {
        return fromString(raw);
    }
}
