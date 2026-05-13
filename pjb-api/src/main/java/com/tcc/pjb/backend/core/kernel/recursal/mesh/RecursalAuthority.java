package com.tcc.pjb.backend.core.kernel.recursal.mesh;

public enum RecursalAuthority {
    JUIZO_SINGULAR(false, false),
    SECRETARIA_JUDICIARIA(false, false),
    PRESIDENCIA(false, true),
    VICE_PRESIDENCIA(false, true),
    RELATOR(false, false),
    CAMARA(true, false),
    TURMA(true, false),
    SECAO(true, false),
    ORGAO_ESPECIAL(true, false),
    PLENARIO(true, false),
    CORTE_ESPECIAL(true, false),
    TRIBUNAL_PLENO(true, false);

    private final boolean colegiado;
    private final boolean presidencia;

    RecursalAuthority(boolean colegiado, boolean presidencia) {
        this.colegiado = colegiado;
        this.presidencia = presidencia;
    }

    public boolean colegiado() {
        return colegiado;
    }

    public boolean presidencia() {
        return presidencia;
    }

    public boolean decisaoMonocratica() {
        return this == JUIZO_SINGULAR || this == RELATOR || this == PRESIDENCIA || this == VICE_PRESIDENCIA;
    }
}
