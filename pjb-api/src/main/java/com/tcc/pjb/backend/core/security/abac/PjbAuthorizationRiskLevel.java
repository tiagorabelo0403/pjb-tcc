package com.tcc.pjb.backend.core.security.abac;

public enum PjbAuthorizationRiskLevel {
    BAIXO,
    MODERADO,
    ALTO,
    CRITICO;

    static PjbAuthorizationRiskLevel fromScore(int score) {
        int normalized = Math.max(0, score);
        if (normalized >= 85) {
            return CRITICO;
        }
        if (normalized >= 60) {
            return ALTO;
        }
        if (normalized >= 30) {
            return MODERADO;
        }
        return BAIXO;
    }
}
