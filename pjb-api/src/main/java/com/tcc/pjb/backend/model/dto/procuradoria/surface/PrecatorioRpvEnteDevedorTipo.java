package com.tcc.pjb.backend.model.dto.procuradoria.surface;

public enum PrecatorioRpvEnteDevedorTipo {
    UNIAO,
    ESTADO,
    DISTRITO_FEDERAL,
    MUNICIPIO,
    AUTARQUIA_FEDERAL,
    AUTARQUIA_ESTADUAL,
    AUTARQUIA_DISTRITAL,
    AUTARQUIA_MUNICIPAL,
    FUNDACAO_PUBLICA_FEDERAL,
    FUNDACAO_PUBLICA_ESTADUAL,
    FUNDACAO_PUBLICA_DISTRITAL,
    FUNDACAO_PUBLICA_MUNICIPAL;

    public boolean federal() {
        return this == UNIAO || this == AUTARQUIA_FEDERAL || this == FUNDACAO_PUBLICA_FEDERAL;
    }

    public boolean subnacional() {
        return !federal();
    }
}
