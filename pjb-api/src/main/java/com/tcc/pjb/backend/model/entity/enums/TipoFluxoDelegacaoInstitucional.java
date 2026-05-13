package com.tcc.pjb.backend.model.entity.enums;

public enum TipoFluxoDelegacaoInstitucional {
    DELEGACAO,
    SUBSTITUICAO;

    public boolean isSubstituicao() {
        return this == SUBSTITUICAO;
    }
}
