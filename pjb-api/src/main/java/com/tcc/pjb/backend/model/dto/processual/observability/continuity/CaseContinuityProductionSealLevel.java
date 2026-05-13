package com.tcc.pjb.backend.model.dto.processual.observability.continuity;

public enum CaseContinuityProductionSealLevel {
    APTO,
    CONDICIONAL,
    BLOQUEADO;

    public boolean isBlocked() {
        return this == BLOQUEADO;
    }

    public boolean allowsSupervisedOperation() {
        return this == APTO || this == CONDICIONAL;
    }
}
