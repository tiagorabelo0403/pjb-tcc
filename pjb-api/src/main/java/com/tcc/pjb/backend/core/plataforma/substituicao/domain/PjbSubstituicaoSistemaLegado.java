package com.tcc.pjb.backend.core.plataforma.substituicao.domain;

public enum PjbSubstituicaoSistemaLegado {
    PJE("PJe"),
    PJE_2X("PJe 2.x"),
    ESAJ("e-SAJ"),
    EPROC("eproc"),
    CRETA("Creta"),
    PROJUDI("Projudi");

    private final String nomePublico;

    PjbSubstituicaoSistemaLegado(String nomePublico) {
        this.nomePublico = nomePublico;
    }

    public String nomePublico() {
        return nomePublico;
    }
}
