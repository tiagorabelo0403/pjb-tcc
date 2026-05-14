package com.tcc.pjb.backend.service.gigs;

public enum GigsActivityType {
    ANALISAR,
    MINUTAR,
    CONFERIR,
    ASSINAR,
    EXPEDIR,
    CERTIFICAR,
    PAUTAR,
    INTIMAR,
    CUMPRIR_PROVIDENCIA,
    VERIFICAR_PERICIA,
    PREPARAR_ACORDO,
    PREPARAR_BAIXA;

    public boolean eAtoJurisdicional() {
        return this == MINUTAR || this == ASSINAR || this == PAUTAR;
    }

    public boolean eAtoCartorario() {
        return this == EXPEDIR || this == CERTIFICAR
                || this == INTIMAR || this == CUMPRIR_PROVIDENCIA;
    }
}
