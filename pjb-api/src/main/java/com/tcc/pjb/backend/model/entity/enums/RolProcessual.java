package com.tcc.pjb.backend.model.entity.enums;

public enum RolProcessual {
    FISCAL_ORDEM_JURIDICA,
    PARTE,
    INTERVENIENTE,
    AUXILIAR_JUSTICA,
    DESTINATARIO_OFICIO,
    APOIO_TECNICO,
    JUIZO_COOPERANTE;

    public PapelProcessualInstitucional toPapelProcessualInstitucional() {
        return switch (this) {
            case FISCAL_ORDEM_JURIDICA -> PapelProcessualInstitucional.FISCAL_ORDEM_JURIDICA;
            case PARTE -> PapelProcessualInstitucional.REPRESENTANTE_JUDICIAL_PARTE;
            case INTERVENIENTE -> PapelProcessualInstitucional.ORGAO_INTERVENIENTE;
            case AUXILIAR_JUSTICA -> PapelProcessualInstitucional.AUXILIAR_JUSTICA;
            case DESTINATARIO_OFICIO -> PapelProcessualInstitucional.DESTINATARIO_OFICIO;
            case APOIO_TECNICO -> PapelProcessualInstitucional.APOIO_TECNICO;
            case JUIZO_COOPERANTE -> PapelProcessualInstitucional.JUIZO_COOPERANTE;
        };
    }
}
