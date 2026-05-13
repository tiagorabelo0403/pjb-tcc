package com.tcc.pjb.backend.model.entity.enums;

public enum StatusIntegracaoInstitucionalExterna {
    PREPARADA,
    ENFILEIRADA,
    ACEITA,
    FALHA_TRANSITORIA,
    FALHA_TERMINAL;

    public boolean isTerminal() {
        return this == ACEITA || this == FALHA_TERMINAL;
    }
}
