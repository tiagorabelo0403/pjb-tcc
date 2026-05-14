package com.tcc.pjb.backend.service.processo.metadata;

public enum ProcessoMetadataIssueType {
    CLASSE_AUSENTE,
    ASSUNTO_AUSENTE,
    PARTE_SEM_DOCUMENTO,
    RITO_INCOMPATIVEL,
    COMPETENCIA_DUVIDOSA,
    VALOR_CAUSA_INCONSISTENTE,
    SIGILO_SEM_JUSTIFICATIVA,
    DOCUMENTO_SEM_TIPO,
    PRAZO_SEM_MARCO;

    public int pontoDesconto() {
        return switch (this) {
            case CLASSE_AUSENTE, PARTE_SEM_DOCUMENTO -> 20;
            case RITO_INCOMPATIVEL, COMPETENCIA_DUVIDOSA -> 15;
            case VALOR_CAUSA_INCONSISTENTE, SIGILO_SEM_JUSTIFICATIVA -> 10;
            case ASSUNTO_AUSENTE, DOCUMENTO_SEM_TIPO, PRAZO_SEM_MARCO -> 5;
        };
    }

    public boolean bloqueante() {
        return this == CLASSE_AUSENTE || this == PARTE_SEM_DOCUMENTO
                || this == RITO_INCOMPATIVEL;
    }
}
