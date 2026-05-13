package com.tcc.pjb.backend.model.entity.enums;

public enum StatusTentativaEntregaInstitucional {
    INICIADA,
    ENCAMINHADA,
    ENTREGUE,
    RETRY_AGENDADO,
    FALHA_TERMINAL;

    public boolean isSucessoOperacional() {
        return this == ENCAMINHADA || this == ENTREGUE;
    }
}
