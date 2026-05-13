package com.tcc.pjb.backend.model.entity.enums;

public enum FuncaoOperacionalInstitucional {
    MEMBRO_TITULAR,
    COORDENADOR_UNIDADE,
    SERVIDOR_TRIAGEM,
    ASSESSOR_INSTITUCIONAL,
    APOIO_TECNICO_SETORIAL,
    GESTOR_CAIXA,
    SUBSTITUTO,
    PLANTONISTA;

    public boolean isLideranca() {
        return this == COORDENADOR_UNIDADE || this == GESTOR_CAIXA;
    }

    public boolean isFuncaoAssinantePreferencial() {
        return switch (this) {
            case MEMBRO_TITULAR, SUBSTITUTO, PLANTONISTA -> true;
            default -> false;
        };
    }
}
