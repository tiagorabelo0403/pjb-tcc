package com.tcc.pjb.backend.model.entity.enums;

public enum InstitutionalAffiliationStatus {
    SOLICITADA,
    EM_VALIDACAO_PJB,
    HOMOLOGADA,
    SUSPENSA,
    REVOGADA;

    public static final InstitutionalAffiliationStatus ATIVA = HOMOLOGADA;

    public boolean isAtiva() {
        return this == HOMOLOGADA;
    }
}
