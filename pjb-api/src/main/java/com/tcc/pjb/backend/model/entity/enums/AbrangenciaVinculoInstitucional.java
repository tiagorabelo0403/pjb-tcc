package com.tcc.pjb.backend.model.entity.enums;

public enum AbrangenciaVinculoInstitucional {
    NACIONAL,
    UF,
    COMARCA,
    UNIDADE;

    public boolean cobre(AbrangenciaVinculoInstitucional requerida) {
        if (requerida == null) {
            return true;
        }
        return ordinal() <= requerida.ordinal();
    }
}
