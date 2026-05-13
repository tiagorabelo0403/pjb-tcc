package com.tcc.pjb.backend.model.entity.enums;

public enum PapelProcessualInstitucional {
    FISCAL_ORDEM_JURIDICA,
    REPRESENTANTE_JUDICIAL_PARTE,
    ORGAO_INTERVENIENTE,
    ORGAO_REQUISITADO,
    AUXILIAR_JUSTICA,
    DESTINATARIO_OFICIO,
    UNIDADE_EXECUTORA,
    APOIO_TECNICO,
    JUIZO_COOPERANTE;

    public boolean exigeCienciaPessoalPreferencial() {
        return switch (this) {
            case FISCAL_ORDEM_JURIDICA,
                    REPRESENTANTE_JUDICIAL_PARTE,
                    ORGAO_INTERVENIENTE,
                    JUIZO_COOPERANTE -> true;
            default -> false;
        };
    }

    public boolean bloqueiaMarcoProcessualSensivel() {
        return switch (this) {
            case FISCAL_ORDEM_JURIDICA,
                    REPRESENTANTE_JUDICIAL_PARTE,
                    AUXILIAR_JUSTICA,
                    JUIZO_COOPERANTE,
                    APOIO_TECNICO -> true;
            default -> false;
        };
    }
}
