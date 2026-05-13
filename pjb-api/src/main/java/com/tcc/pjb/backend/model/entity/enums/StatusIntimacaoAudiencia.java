package com.tcc.pjb.backend.model.entity.enums;

public enum StatusIntimacaoAudiencia {
    PENDENTE,
    ENVIADA,
    CIENCIA_CONFIRMADA,
    PRAZO_EXPIRADO,
    FALHOU;

    public boolean isTerminal() {
        return this == CIENCIA_CONFIRMADA || this == PRAZO_EXPIRADO || this == FALHOU;
    }
}
