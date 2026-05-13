package com.tcc.pjb.backend.model.entity.enums;

public enum StatusMinutaInstitucional {
    RASCUNHO,
    EM_APROVACAO,
    APROVADA,
    REJEITADA,
    ENVIADA;

    public boolean isPendenteAprovacao() {
        return this == EM_APROVACAO;
    }

    public boolean isTerminal() {
        return this == APROVADA || this == REJEITADA || this == ENVIADA;
    }
}
