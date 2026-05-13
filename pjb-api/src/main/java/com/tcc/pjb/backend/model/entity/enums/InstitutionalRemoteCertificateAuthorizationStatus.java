package com.tcc.pjb.backend.model.entity.enums;

public enum InstitutionalRemoteCertificateAuthorizationStatus {
    ATIVA,
    REVOGADA,
    EXPIRADA;

    public boolean isAtiva() {
        return this == ATIVA;
    }
}
