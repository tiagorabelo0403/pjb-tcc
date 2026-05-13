package com.tcc.pjb.backend.model.entity.enums;

public enum GrauConfiancaVinculoProcessual {
    DETERMINISTICO,
    PROVAVEL,
    PENDENTE_CONFIRMACAO;

    public boolean confirmado() {
        return this == DETERMINISTICO;
    }

    public boolean elegivelPainelPessoal() {
        return this != PENDENTE_CONFIRMACAO;
    }
}
