package com.tcc.pjb.backend.service.processo.autuacao;

public enum RetificacaoAutuacaoStatus {
    RASCUNHO,
    AGUARDANDO_CONFERENCIA,
    APROVADA,
    REJEITADA,
    APLICADA,
    CANCELADA;

    public boolean eEditavel() {
        return this == RASCUNHO;
    }

    public boolean eTerminal() {
        return this == APLICADA || this == REJEITADA || this == CANCELADA;
    }
}
