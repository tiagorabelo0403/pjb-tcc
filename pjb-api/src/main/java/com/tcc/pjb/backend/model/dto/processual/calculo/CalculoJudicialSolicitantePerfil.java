package com.tcc.pjb.backend.model.dto.processual.calculo;

public enum CalculoJudicialSolicitantePerfil {
    CIDADAO,
    ADVOGADO,
    MAGISTRATURA,
    CONTADOR_JUDICIAL,
    PROCURADORIA,
    TECNICO_INSTITUCIONAL;

    public boolean citizenLike() {
        return this == CIDADAO;
    }

    public boolean technicalLike() {
        return this == ADVOGADO || this == MAGISTRATURA || this == CONTADOR_JUDICIAL || this == PROCURADORIA || this == TECNICO_INSTITUCIONAL;
    }
}
