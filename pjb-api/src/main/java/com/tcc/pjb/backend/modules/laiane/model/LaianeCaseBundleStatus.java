package com.tcc.pjb.backend.modules.laiane.model;

import com.tcc.pjb.backend.core.util.EnumText;

public enum LaianeCaseBundleStatus {
    ABERTO,
    EM_ANALISE,
    CONSOLIDADO,
    ARQUIVADO;

    
    public static LaianeCaseBundleStatus from(String raw) {
        if (raw == null) return ABERTO;
        String v = raw.trim();
        if (v.isEmpty()) return ABERTO;

        String token = EnumText.normalizeToken(v);
        if (token.isBlank()) return ABERTO;

        token = switch (token) {
            case "EM_ANALISE", "ANALISE", "EM_REVISAO" -> "EM_ANALISE";
            case "CONSOLIDAR" -> "CONSOLIDADO";
            case "ARQUIVAR" -> "ARQUIVADO";
            default -> token;
        };

        try {
            return LaianeCaseBundleStatus.valueOf(token);
        } catch (Exception ignored) {
            return ABERTO;
        }
    }
}
