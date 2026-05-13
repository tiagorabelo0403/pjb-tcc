package com.tcc.pjb.backend.core.processo.lifecycle;

public enum ProcessoLifecycleAction {
    DISTRIBUIR,
    EXPEDIR_INTIMACAO,
    REALIZAR_JUNTADA,
    CONCLUIR_PARA_DESPACHO,
    DESIGNAR_AUDIENCIA,
    ASSINAR_DESPACHO,
    PROFERIR_SENTENCA,
    INTERPOR_RECURSO,
    PROFERIR_VOTO,
    LAVRAR_ACORDAO,
    PEDIR_VISTA,
    CERTIFICAR_TRANSITO,
    INICIAR_CUMPRIMENTO,
    ARQUIVAR,
    DESARQUIVAR,
    REGISTRAR_ACORDO,
    APRESENTAR_LAUDO,
    RESPONDER_QUESITOS;

    public boolean isRecursal() {
        return switch (this) {
            case INTERPOR_RECURSO, PROFERIR_VOTO, LAVRAR_ACORDAO, PEDIR_VISTA, CERTIFICAR_TRANSITO -> true;
            default -> false;
        };
    }

    public boolean isInstrutoria() {
        return switch (this) {
            case DESIGNAR_AUDIENCIA, APRESENTAR_LAUDO, RESPONDER_QUESITOS -> true;
            default -> false;
        };
    }

    public boolean isTerminal() {
        return this == CERTIFICAR_TRANSITO || this == ARQUIVAR;
    }

    public boolean requiresMagistrature() {
        return switch (this) {
            case DESIGNAR_AUDIENCIA,
                    ASSINAR_DESPACHO,
                    PROFERIR_SENTENCA,
                    PROFERIR_VOTO,
                    LAVRAR_ACORDAO,
                    PEDIR_VISTA,
                    INICIAR_CUMPRIMENTO -> true;
            default -> false;
        };
    }

    public boolean isSensitiveJudicial() {
        return switch (this) {
            case ASSINAR_DESPACHO,
                    PROFERIR_SENTENCA,
                    PROFERIR_VOTO,
                    LAVRAR_ACORDAO,
                    CERTIFICAR_TRANSITO,
                    INICIAR_CUMPRIMENTO,
                    ARQUIVAR -> true;
            default -> false;
        };
    }

    public boolean requiresCaseContinuityGate() {
        return isSensitiveJudicial() || isRecursal() || isTerminal();
    }

    public boolean isProductionSealCritical() {
        return switch (this) {
            case ASSINAR_DESPACHO,
                    PROFERIR_SENTENCA,
                    PROFERIR_VOTO,
                    LAVRAR_ACORDAO,
                    CERTIFICAR_TRANSITO,
                    INICIAR_CUMPRIMENTO,
                    ARQUIVAR -> true;
            default -> false;
        };
    }

    public String operationalAxis() {
        return switch (this) {
            case DISTRIBUIR -> "DISTRIBUICAO";
            case EXPEDIR_INTIMACAO, REALIZAR_JUNTADA, CONCLUIR_PARA_DESPACHO -> "SECRETARIA";
            case DESIGNAR_AUDIENCIA, APRESENTAR_LAUDO, RESPONDER_QUESITOS -> "INSTRUCAO";
            case ASSINAR_DESPACHO, PROFERIR_SENTENCA, REGISTRAR_ACORDO -> "JULGAMENTO";
            case INTERPOR_RECURSO, PROFERIR_VOTO, LAVRAR_ACORDAO, PEDIR_VISTA, CERTIFICAR_TRANSITO -> "RECURSAL";
            case INICIAR_CUMPRIMENTO -> "EXECUCAO";
            case ARQUIVAR, DESARQUIVAR -> "GESTAO_ACERVO";
        };
    }
}
