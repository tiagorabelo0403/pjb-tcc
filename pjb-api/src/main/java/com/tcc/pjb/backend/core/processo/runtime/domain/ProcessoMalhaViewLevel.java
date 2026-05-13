package com.tcc.pjb.backend.core.processo.runtime.domain;

public enum ProcessoMalhaViewLevel {
    PUBLICO,
    RESTRITO,
    PLENO;

    public boolean isRestrito() {
        return this == RESTRITO;
    }

    public boolean isPleno() {
        return this == PLENO;
    }
}
