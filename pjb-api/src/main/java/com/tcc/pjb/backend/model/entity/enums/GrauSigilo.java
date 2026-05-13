package com.tcc.pjb.backend.model.entity.enums;


public enum GrauSigilo {

    PUBLICO(0),
    RESTRITO(1),
    SIGILOSO(2),
    ULTRASSECRETO(3);

    private final int nivel;

    GrauSigilo(int nivel) {
        this.nivel = nivel;
    }

    public boolean isMaisRestritivoQue(GrauSigilo outro) {
        if (outro == null) return true;
        return this.nivel > outro.nivel;
    }

    public int getNivel() {
        return nivel;
    }
}
