package com.tcc.pjb.backend.core.security.scope;

public class AcessoForaDeEscopoException extends RuntimeException {

    private final MotivoNegacaoEscopo motivo;
    private final TipoObjetoProtegido tipoObjeto;
    private final Long objetoId;

    public AcessoForaDeEscopoException(MotivoNegacaoEscopo motivo, TipoObjetoProtegido tipoObjeto, Long objetoId) {
        super("Acesso negado: " + motivo.name());
        this.motivo = motivo;
        this.tipoObjeto = tipoObjeto;
        this.objetoId = objetoId;
    }

    public MotivoNegacaoEscopo getMotivo() {
        return motivo;
    }

    public TipoObjetoProtegido getTipoObjeto() {
        return tipoObjeto;
    }

    public Long getObjetoId() {
        return objetoId;
    }
}
