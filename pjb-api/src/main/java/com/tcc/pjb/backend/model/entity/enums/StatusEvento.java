package com.tcc.pjb.backend.model.entity.enums;

public enum StatusEvento {

    PENDENTE("Evento criado, ainda não concluído"),
    EM_ANDAMENTO("Evento em execução ou audiência em curso"),
    CONCLUIDO("Evento concluído com êxito"),
    CANCELADO("Evento cancelado antes da conclusão"),
    EXPIRADO("Evento expirado por decurso de prazo"),
    REAGENDADO("Evento reagendado para nova data"),
    AGUARDANDO_CONFIRMACAO("Evento criado, aguardando confirmação de partes ou juízo");

    private final String descricao;

    StatusEvento(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
