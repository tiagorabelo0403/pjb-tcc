package com.tcc.pjb.backend.model.entity.enums;

public enum StatusDelegacaoInstitucional {
    ATIVA,
    REVOGADA,
    EXPIRADA,
    SUBSTITUIDA;

    public boolean isAtiva() {
        return this == ATIVA;
    }

    public boolean isTerminal() {
        return this == REVOGADA || this == EXPIRADA || this == SUBSTITUIDA;
    }
}
