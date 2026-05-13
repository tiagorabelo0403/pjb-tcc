package com.tcc.pjb.backend.model.entity.enums;

public enum InstitutionalNominationStatus {
    PROPOSTA,
    ATIVA,
    SUSPENSA,
    REVOGADA,
    EXPIRADA;

    public boolean isAtiva() {
        return this == ATIVA;
    }
}
