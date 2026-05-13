package com.tcc.pjb.backend.model.entity.enums;

public enum StatusCoberturaOperacionalInstitucional {
    ATIVA,
    PAUSADA,
    ENCERRADA;

    public boolean isAtiva() {
        return this == ATIVA;
    }
}
