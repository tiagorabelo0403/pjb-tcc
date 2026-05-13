package com.tcc.pjb.backend.model.entity.workflow;

public enum MovimentacaoAdjustmentMode {
    RETIFICACAO,
    DESCONSIDERACAO_LOGICA;

    public static MovimentacaoAdjustmentMode parse(String value) {
        if (value == null || value.isBlank()) {
            return RETIFICACAO;
        }
        return switch (value.trim().toUpperCase()) {
            case "DESCONSIDERACAO_LOGICA", "EXCLUSAO_LOGICA", "CANCELAMENTO_LOGICO" -> DESCONSIDERACAO_LOGICA;
            default -> RETIFICACAO;
        };
    }
}
