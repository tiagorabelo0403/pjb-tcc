package com.tcc.pjb.backend.model.entity.enums;

public enum MotivoFalhaEntregaInstitucional {
    INTEGRACAO_INDISPONIVEL,
    RESPOSTA_INVALIDA,
    CANAL_NAO_SUPORTADO,
    UNIDADE_SEM_CANAL,
    ERRO_TRANSITORIO,
    EXAURIDA_POLITICA_RETRY,
    FALHA_TERMINAL_CANAL,
    OUTRO;

    public boolean isNormalmenteTransitavel() {
        return switch (this) {
            case INTEGRACAO_INDISPONIVEL, RESPOSTA_INVALIDA, ERRO_TRANSITORIO -> true;
            default -> false;
        };
    }
}
