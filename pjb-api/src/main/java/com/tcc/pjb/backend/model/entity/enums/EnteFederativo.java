package com.tcc.pjb.backend.model.entity.enums;


public enum EnteFederativo {

    MUNICIPIO,
    ESTADO,
    UNIAO;

    

    public boolean isMunicipal() {
        return this == MUNICIPIO;
    }

    public boolean isEstadual() {
        return this == ESTADO;
    }

    public boolean isFederal() {
        return this == UNIAO;
    }
}
