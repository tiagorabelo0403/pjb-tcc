package com.tcc.pjb.backend.modules.laiane.model;

import com.tcc.pjb.backend.core.util.EnumText;

public enum LaianeOficioStatus {
    CRIADO,
    ENVIADO,
    ENTREGUE,
    CANCELADO;

    
    public static LaianeOficioStatus from(String raw) {
        if (raw == null) return CRIADO;
        String v = raw.trim();
        if (v.isEmpty()) return CRIADO;

        String token = EnumText.normalizeToken(v);
        if (token.isBlank()) return CRIADO;

        
        token = switch (token) {
            case "CRIAR", "NOVO", "NOVO_OFICIO" -> "CRIADO";
            default -> token;
        };

        try {
            return LaianeOficioStatus.valueOf(token);
        } catch (Exception ignored) {
            return CRIADO;
        }
    }
}
