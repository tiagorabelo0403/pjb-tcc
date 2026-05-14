package com.tcc.pjb.backend.service.audiencia;

public enum AudienciaTipo {
    CONCILIACAO,
    INSTRUCAO_E_JULGAMENTO,
    MEDIACAO,
    UNA,
    OITIVA_TESTEMUNHA,
    OITIVA_PARTE,
    PERICIAL,
    SUSTENTACAO_ORAL,
    JULGAMENTO_COLEGIADO,
    CUSTODIAL,
    ADMONITORIA,
    ESPECIAL_MENORES,
    VIRTUAL;

    public boolean admiteVirtual() {
        return switch (this) {
            case CUSTODIAL, ESPECIAL_MENORES -> false;
            default -> true;
        };
    }

    public boolean exigePresencaFisica() {
        return this == CUSTODIAL || this == ESPECIAL_MENORES;
    }

    public boolean permiteGravacao() {
        return this != ESPECIAL_MENORES;
    }

    public int duracaoEstimadaMinutos() {
        return switch (this) {
            case CONCILIACAO, MEDIACAO -> 60;
            case INSTRUCAO_E_JULGAMENTO, UNA -> 120;
            case PERICIAL -> 90;
            case JULGAMENTO_COLEGIADO -> 30;
            case SUSTENTACAO_ORAL -> 20;
            default -> 45;
        };
    }
}
