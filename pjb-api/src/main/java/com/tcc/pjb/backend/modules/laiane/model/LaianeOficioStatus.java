package com.tcc.pjb.backend.modules.laiane.model;

import com.tcc.pjb.backend.core.util.EnumText;

public enum LaianeOficioStatus {
    CRIADO {
        @Override
        public boolean canTransitionTo(LaianeOficioStatus target) {
            return target == ENVIADO || target == CANCELADO;
        }
    },
    ENVIADO {
        @Override
        public boolean canTransitionTo(LaianeOficioStatus target) {
            return target == ENTREGUE || target == CANCELADO;
        }
    },
    ENTREGUE {
        @Override
        public boolean canTransitionTo(LaianeOficioStatus target) {
            return false;
        }
    },
    CANCELADO {
        @Override
        public boolean canTransitionTo(LaianeOficioStatus target) {
            return false;
        }
    };

    public abstract boolean canTransitionTo(LaianeOficioStatus target);

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
