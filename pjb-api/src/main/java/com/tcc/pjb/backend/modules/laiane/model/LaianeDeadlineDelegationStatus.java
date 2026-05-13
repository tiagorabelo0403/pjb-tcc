package com.tcc.pjb.backend.modules.laiane.model;

import com.tcc.pjb.backend.core.util.EnumText;

public enum LaianeDeadlineDelegationStatus {
    PENDENTE,
    ACEITA,
    CONCLUIDA,
    CANCELADA;

    
    public static LaianeDeadlineDelegationStatus from(String raw) {
        if (raw == null) return PENDENTE;
        String v = raw.trim();
        if (v.isEmpty()) return PENDENTE;

        String token = EnumText.normalizeToken(v);
        if (token.isBlank()) return PENDENTE;

        token = switch (token) {
            case "ACEITO" -> "ACEITA";
            case "CONCLUIDO" -> "CONCLUIDA";
            case "CANCELADO" -> "CANCELADA";
            default -> token;
        };

        try {
            return LaianeDeadlineDelegationStatus.valueOf(token);
        } catch (Exception ignored) {
            return PENDENTE;
        }
    }
}
