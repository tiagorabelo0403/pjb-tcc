package com.tcc.pjb.backend.core.dje.domain;

public record DjePublicacaoCommand(Long processoId,
                                    String tipoAto,
                                    String conteudo,
                                    String tribunalCodigo) {
}
