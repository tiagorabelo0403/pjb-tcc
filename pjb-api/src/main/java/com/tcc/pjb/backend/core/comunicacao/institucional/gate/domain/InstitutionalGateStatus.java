package com.tcc.pjb.backend.core.comunicacao.institucional.gate.domain;

public enum InstitutionalGateStatus {
    AGUARDANDO_CIENCIA,
    AGUARDANDO_CUMPRIMENTO,
    LIBERADO;

    public boolean isBloqueado() {
        return this != LIBERADO;
    }
}
