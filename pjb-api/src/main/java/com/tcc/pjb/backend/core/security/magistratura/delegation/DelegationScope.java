package com.tcc.pjb.backend.core.security.magistratura.delegation;

import java.util.Locale;

public enum DelegationScope {

    
    READ_ONLY,

    
    READ_WRITE_DRAFT;

    public static DelegationScope parseLenient(String raw) {
        if (raw == null) return null;
        String v = raw.trim();
        if (v.isBlank()) return null;
        v = v.toUpperCase(Locale.ROOT);
        try {
            return DelegationScope.valueOf(v);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public boolean canReadPanels() {
        return true;
    }

    public boolean canDraft() {
        return this == READ_WRITE_DRAFT;
    }

    
    public static DelegationScope parse(String raw) {
        return parseLenient(raw);
    }
}
