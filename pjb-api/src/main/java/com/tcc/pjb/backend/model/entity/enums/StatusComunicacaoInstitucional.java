package com.tcc.pjb.backend.model.entity.enums;

public enum StatusComunicacaoInstitucional {
    CRIADA,
    RESOLVIDA,
    DISPONIBILIZADA,
    RECEBIDA,
    CIENTIFICADA,
    CUMPRIDA,
    EM_FALLBACK,
    FRUSTRADA;

    public boolean isTerminal() {
        return this == CUMPRIDA || this == FRUSTRADA;
    }
}
