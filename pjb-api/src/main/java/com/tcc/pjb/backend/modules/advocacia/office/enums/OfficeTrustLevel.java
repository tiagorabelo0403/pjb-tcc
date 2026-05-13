package com.tcc.pjb.backend.modules.advocacia.office.enums;

public enum OfficeTrustLevel {
    RESTRITO,
    CONTROLADO,
    ELEVADO,
    PATRONO;

    public static OfficeTrustLevel fromScore(int score) {
        if (score <= 2) {
            return RESTRITO;
        }
        if (score <= 5) {
            return CONTROLADO;
        }
        if (score <= 7) {
            return ELEVADO;
        }
        return PATRONO;
    }
}
