package com.tcc.pjb.backend.model.entity.enums;

public enum StatusAcordo {
    EM_NEGOCIACAO(false),
    AGUARDANDO_REVISAO_HUMANA(false),
    AGUARDANDO_ASSINATURA_PARTE1(false),
    AGUARDANDO_ASSINATURA_PARTE2(false),
    AGUARDANDO_HOMOLOGACAO_JUIZ(false),
    HOMOLOGADO(true),
    REJEITADO_PELO_JUIZ(true),
    CANCELADO(true),
    RASCUNHO(false);
    private final boolean isTerminal;

    StatusAcordo(boolean isTerminal) {
        this.isTerminal = isTerminal;
    }

    public boolean isTerminal() {
        return this.isTerminal;
    }
}