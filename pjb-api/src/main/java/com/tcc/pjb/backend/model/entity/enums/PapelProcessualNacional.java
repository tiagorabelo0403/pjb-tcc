package com.tcc.pjb.backend.model.entity.enums;

public enum PapelProcessualNacional {
    AUTOR,
    REU,
    VITIMA,
    INVESTIGADO,
    EXECUTANTE,
    EXECUTADO,
    IMPETRANTE,
    IMPETRADO,
    INTERESSADO,
    TERCEIRO_INTERESSADO,
    ADVOGADO,
    REPRESENTANTE_LEGAL,
    MEMBRO_MINISTERIO_PUBLICO,
    DEFENSOR_PUBLICO,
    PROCURADOR_PUBLICO,
    PERITO,
    TESTEMUNHA,
    ASSISTENTE,
    AUTORIDADE,
    SUJEITO_PROCESSUAL;

    public boolean partePrincipal() {
        return switch (this) {
            case AUTOR, REU, VITIMA, INVESTIGADO, EXECUTANTE, EXECUTADO, IMPETRANTE, IMPETRADO, INTERESSADO, TERCEIRO_INTERESSADO -> true;
            default -> false;
        };
    }

    public int prioridadePainel() {
        return switch (this) {
            case AUTOR, REU, VITIMA, INVESTIGADO, EXECUTANTE, EXECUTADO, IMPETRANTE, IMPETRADO -> 0;
            case INTERESSADO, TERCEIRO_INTERESSADO, SUJEITO_PROCESSUAL -> 1;
            case ADVOGADO, REPRESENTANTE_LEGAL, MEMBRO_MINISTERIO_PUBLICO, DEFENSOR_PUBLICO, PROCURADOR_PUBLICO -> 2;
            case PERITO, TESTEMUNHA, ASSISTENTE, AUTORIDADE -> 3;
        };
    }
}
