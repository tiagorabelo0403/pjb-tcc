package com.tcc.pjb.backend.model.entity.enums.document;

public enum DocumentoEstadoOperacional {
    RASCUNHO,
    SALVO,
    FINALIZADO,
    AGUARDANDO_LIBERACAO,
    LIBERADO_AUTOS_DIGITAIS,
    AGUARDANDO_ASSINATURA,
    ASSINADO,
    PROTOCOLADO,
    TORNADO_SEM_EFEITO,
    RECLASSIFICADO,
    SIGILOSO;

    public boolean eEditavel() {
        return this == RASCUNHO || this == SALVO;
    }

    public boolean ePublicado() {
        return this == LIBERADO_AUTOS_DIGITAIS || this == PROTOCOLADO;
    }

    public boolean eAssinavel() {
        return this == AGUARDANDO_ASSINATURA || this == FINALIZADO;
    }
}
