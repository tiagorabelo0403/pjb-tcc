package com.tcc.pjb.backend.core.comunicacao.judicial;

public enum ComunicacaoJudicialTribunalSuperior {
    NENHUM,
    STJ,
    STF,
    TST,
    TSE,
    STM,
    SUPERIOR_GENERICO,
    CONSTITUCIONAL_GENERICO;

    public boolean isSuperior() {
        return this != NENHUM;
    }

    public boolean isConstitucional() {
        return this == STF || this == CONSTITUCIONAL_GENERICO;
    }
}
