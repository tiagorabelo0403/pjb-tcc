package com.tcc.pjb.backend.service.custas;

public enum TipoCusta {
    PREPARO_RECURSAL,
    CUSTAS_INICIAIS,
    PERITO_ADIANTAMENTO,
    MULTA_LITIGANCIA_MA_FE,
    MULTA_IMPROBIDADE_ART77,
    MULTA_ART_1026_CPC,
    HONORARIOS_PERICIAIS,
    EMOLUMENTO_CARTORIAL,
    CUSTAS_EXPEDICAO_MANDADO;

    public boolean eMulta() {
        return this == MULTA_LITIGANCIA_MA_FE
                || this == MULTA_IMPROBIDADE_ART77
                || this == MULTA_ART_1026_CPC;
    }

    public boolean requerDespacho() {
        return this == MULTA_LITIGANCIA_MA_FE
                || this == MULTA_IMPROBIDADE_ART77;
    }
}
