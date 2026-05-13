package com.tcc.pjb.backend.model.entity.enums;

public enum TelemetriaOperacionalCanal {
    OFICIAL_JUSTICA,
    DELEGADO;

    public boolean isOficialJustica() {
        return this == OFICIAL_JUSTICA;
    }

    public boolean isDelegado() {
        return this == DELEGADO;
    }
}
