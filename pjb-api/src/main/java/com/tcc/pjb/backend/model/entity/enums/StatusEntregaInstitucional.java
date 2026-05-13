package com.tcc.pjb.backend.model.entity.enums;

public enum StatusEntregaInstitucional {
    PENDENTE,
    EM_PROCESSAMENTO,
    ENCAMINHADA,
    ENTREGUE,
    AGUARDANDO_RETRY,
    FALHA_TERMINAL,
    MOVIDA_DLQ;

    public boolean isDespachavel() {
        return this == PENDENTE || this == AGUARDANDO_RETRY;
    }

    public boolean isTerminal() {
        return this == ENCAMINHADA || this == ENTREGUE || this == FALHA_TERMINAL || this == MOVIDA_DLQ;
    }
}
