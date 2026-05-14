package com.tcc.pjb.backend.service.recurso;

public enum RecursoProcessualTipo {
    APELACAO,
    AGRAVO_DE_INSTRUMENTO,
    AGRAVO_INTERNO,
    AGRAVO_REGIMENTAL,
    EMBARGOS_DECLARACAO,
    EMBARGOS_INFRINGENTES,
    RECURSO_ESPECIAL,
    RECURSO_ESPECIAL_REPETITIVO,
    RECURSO_EXTRAORDINARIO,
    RECURSO_EXTRAORDINARIO_REPERCUSSAO_GERAL,
    RECURSO_ORDINARIO,
    RECURSO_INOMINADO_JEC,
    RECURSO_EM_SENTIDO_ESTRITO;

    public int prazoBaseDias() {
        return switch (this) {
            case EMBARGOS_DECLARACAO -> 5;
            case RECURSO_INOMINADO_JEC -> 10;
            default -> 15;
        };
    }

    public boolean admiteEfetoSuspensivo() {
        return this == APELACAO;
    }

    public boolean exigePreparo() {
        return switch (this) {
            case EMBARGOS_DECLARACAO, RECURSO_INOMINADO_JEC -> false;
            default -> true;
        };
    }

    public boolean ehExtraordinario() {
        return this == RECURSO_ESPECIAL
                || this == RECURSO_ESPECIAL_REPETITIVO
                || this == RECURSO_EXTRAORDINARIO
                || this == RECURSO_EXTRAORDINARIO_REPERCUSSAO_GERAL;
    }
}
