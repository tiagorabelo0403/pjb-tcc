package com.tcc.pjb.backend.service.prazo;

public enum PrazoTipo {
    PROCESSUAL_PROPRIO,
    PROCESSUAL_IMPRÓPRIO,
    PEREMPTÓRIO,
    DILATÓRIO,
    LEGAL,
    JUDICIAL,
    CONVENCIONAL;

    public boolean eFatal() {
        return this == PEREMPTÓRIO || this == LEGAL;
    }

    public boolean admiteProrogacao() {
        return this == DILATÓRIO || this == JUDICIAL || this == CONVENCIONAL;
    }
}
