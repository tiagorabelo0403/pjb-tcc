package com.tcc.pjb.backend.core.dje.domain;

public record DjePublicacaoPayload(Long processoId, String tipoAto, String conteudo, String tribunalCodigo) {
    public DjePublicacaoCommand toCommand() {
        return new DjePublicacaoCommand(processoId, tipoAto, conteudo, tribunalCodigo);
    }
}
