package com.tcc.pjb.backend.model.entity.enums;

import com.tcc.pjb.backend.core.util.EnumText;

public enum ModuloProcesso {
    CIVIL,
    PENAL,
    TRABALHISTA,
    ELEITORAL,
    MILITAR,
    AMBIENTAL,
    TRIBUTARIO,
    FAMILIA,
    PREVIDENCIARIO,
    CONSUMIDOR,
    FAZENDARIA,
    INFANCIA_JUVENTUDE,
    REGISTROS_PUBLICOS,
    FALENCIAS;

    
    public static ModuloProcesso fromString(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String token = EnumText.normalizeToken(raw);
        if (token.isBlank()) return null;

        token = switch (token) {
            case "FAZENDA", "FAZENDARIO", "FAZENDA_PUBLICA" -> "FAZENDARIA";
            case "INFANCIA", "JUVENTUDE", "INFANCIA_E_JUVENTUDE" -> "INFANCIA_JUVENTUDE";
            case "REGISTRO", "REGISTROS", "REGISTRO_PUBLICO" -> "REGISTROS_PUBLICOS";
            case "TRIBUTARIA" -> "TRIBUTARIO";
            case "PREVIDENCIARIA" -> "PREVIDENCIARIO";
            default -> token;
        };

        try {
            return ModuloProcesso.valueOf(token);
        } catch (Exception ignored) {
            return null;
        }
    }
}
