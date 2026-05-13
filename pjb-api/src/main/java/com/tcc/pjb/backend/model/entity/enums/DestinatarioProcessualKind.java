package com.tcc.pjb.backend.model.entity.enums;

public enum DestinatarioProcessualKind {
    PESSOA_FISICA,
    PESSOA_JURIDICA,
    ADVOGADO,
    PARTE,
    TERCEIRO,
    AUXILIAR_JUSTICA,
    ORGAO_INSTITUCIONAL,
    UNIDADE_INSTITUCIONAL;

    public boolean isInstitucional() {
        return this == ORGAO_INSTITUCIONAL || this == UNIDADE_INSTITUCIONAL;
    }

    public boolean isPessoalOuRepresentacional() {
        return !isInstitucional();
    }
}
