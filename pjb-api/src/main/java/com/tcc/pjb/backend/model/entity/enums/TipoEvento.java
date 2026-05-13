package com.tcc.pjb.backend.model.entity.enums;

public enum TipoEvento {
    PRAZO_LEGAL("Prazo processual automático"),
    CITACAO("Citação"),
    INTIMACAO("Intimação"),
    NOTIFICACAO("Notificação"),
    AUDIENCIA_CONCILIACAO("Audiência de conciliação"),
    AUDIENCIA_INSTRUCAO("Audiência de instrução e julgamento"),
    DESPACHO_JUDICIAL("Despacho proferido pelo juiz"),
    DECISAO_INTERLOCUTORIA("Decisão interlocutória emitida"),
    SENTENCA("Sentença proferida"),
    RECURSO("Recurso interposto"),
    ARQUIVAMENTO("Evento de arquivamento");

    private final String descricao;

    TipoEvento(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
