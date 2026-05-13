package com.tcc.pjb.backend.model.entity.enums;

public enum InstitutionalAffiliationRequestStatus {
    RASCUNHO,
    PENDENTE_VALIDACAO,
    EM_HOMOLOGACAO,
    HOMOLOGADA,
    REJEITADA,
    REVOGADA;

    public boolean isAtiva() {
        return this == PENDENTE_VALIDACAO || this == EM_HOMOLOGACAO || this == HOMOLOGADA;
    }

    public boolean isTerminal() {
        return this == HOMOLOGADA || this == REJEITADA || this == REVOGADA;
    }
}
