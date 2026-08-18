package com.tcc.pjb.backend.integration.serpro.datavalid;

public enum CpfSituacao {

    REGULAR,
    SUSPENSA,
    CANCELADA_POR_OBITO,
    PENDENTE_DE_REGULARIZACAO,
    CANCELADA_POR_MULTIPLICIDADE,
    NULA,
    NAO_ENCONTRADA;

    public boolean bloqueante() {
        return this != REGULAR;
    }
}
