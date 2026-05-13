package com.tcc.pjb.backend.model.entity.enums;

public enum InstitutionalIntegrationCredentialStatus {
    ATIVA,
    ROTACIONADA,
    REVOGADA,
    EXPIRADA;

    public boolean isAtiva() {
        return this == ATIVA || this == ROTACIONADA;
    }
}
