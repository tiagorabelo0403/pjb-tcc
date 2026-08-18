package com.tcc.pjb.backend.modules.custas.domain;

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

    public boolean aplicaAoAjuizamentoInicial() {
        return this == CUSTAS_INICIAIS;
    }

    public boolean aplicaAoRecursal() {
        return this == PREPARO_RECURSAL;
    }

    public String fundamentoLegal() {
        return switch (this) {
            case CUSTAS_INICIAIS -> "CPC, art. 82";
            case PREPARO_RECURSAL -> "CPC, art. 1.007";
            case PERITO_ADIANTAMENTO -> "CPC, art. 95";
            case HONORARIOS_PERICIAIS -> "CPC, art. 465";
            case MULTA_LITIGANCIA_MA_FE -> "CPC, art. 81";
            case MULTA_IMPROBIDADE_ART77 -> "CPC, art. 77, § 2º";
            case MULTA_ART_1026_CPC -> "CPC, art. 1.026, § 2º";
            case EMOLUMENTO_CARTORIAL -> "Lei 10.169/2000";
            case CUSTAS_EXPEDICAO_MANDADO -> "CPC, art. 82, § 1º";
        };
    }
}
