package com.tcc.pjb.backend.modules.balcao.entity;

public enum BalcaoAtendimentoStatus {
    AGUARDANDO,
    CHAMADO,
    EM_ATENDIMENTO,
    CONCLUIDO,
    NAO_COMPARECEU,
    CANCELADO;

    public boolean isAtivo() {
        return this == AGUARDANDO || this == CHAMADO || this == EM_ATENDIMENTO;
    }

    public boolean isTerminal() {
        return this == CONCLUIDO || this == NAO_COMPARECEU || this == CANCELADO;
    }
}
