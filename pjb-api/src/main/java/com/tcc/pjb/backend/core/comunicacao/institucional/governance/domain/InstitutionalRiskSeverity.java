package com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain;

public enum InstitutionalRiskSeverity {
    BAIXA(10),
    MEDIA(25),
    ALTA(50),
    CRITICA(100);

    private final int weight;

    InstitutionalRiskSeverity(int weight) {
        this.weight = weight;
    }

    public int weight() {
        return weight;
    }

    public boolean isBlocking() {
        return this == CRITICA;
    }
}
