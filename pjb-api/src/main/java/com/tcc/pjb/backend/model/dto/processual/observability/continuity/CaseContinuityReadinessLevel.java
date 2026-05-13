package com.tcc.pjb.backend.model.dto.processual.observability.continuity;

public enum CaseContinuityReadinessLevel {
    SAUDAVEL,
    ALERTA,
    CRITICA;

    public boolean isCritical() {
        return this == CRITICA;
    }

    public boolean requiresImmediateAction() {
        return this == ALERTA || this == CRITICA;
    }

    public boolean allowsConditionalRelease() {
        return this == SAUDAVEL || this == ALERTA;
    }
}
