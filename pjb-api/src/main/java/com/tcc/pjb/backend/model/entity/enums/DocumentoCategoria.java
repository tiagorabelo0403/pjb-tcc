package com.tcc.pjb.backend.model.entity.enums;

import java.util.Map;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.tcc.pjb.backend.core.util.EnumText;

public enum DocumentoCategoria {

    
    PUBLICO,

    
    PESSOAL;

    private static final Map<String, DocumentoCategoria> ALIASES = Map.ofEntries(
            Map.entry("PUBLIC", PUBLICO),
            Map.entry("PUBLICO", PUBLICO),
            Map.entry("PUB", PUBLICO),
            Map.entry("PESSOAL", PESSOAL),
            Map.entry("PERSONAL", PESSOAL),
            Map.entry("SENSIVEL", PESSOAL),
            Map.entry("SENSITIVE", PESSOAL)
    );

    public static DocumentoCategoria fromString(String raw) {
        if (raw == null || raw.isBlank()) return PUBLICO;
        String token = EnumText.normalizeToken(raw);
        if (token.isBlank()) return PUBLICO;
        DocumentoCategoria a = ALIASES.get(token);
        if (a != null) return a;
        try {
            return DocumentoCategoria.valueOf(token);
        } catch (Exception ignored) {
            return PUBLICO;
        }
    }

    @JsonCreator
    public static DocumentoCategoria jsonCreator(String raw) {
        return fromString(raw);
    }
}
