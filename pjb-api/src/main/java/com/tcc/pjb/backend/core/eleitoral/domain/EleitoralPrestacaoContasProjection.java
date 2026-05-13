package com.tcc.pjb.backend.core.eleitoral.domain;

public record EleitoralPrestacaoContasProjection(Long processoId,
                                                 String partidoSigla,
                                                 String cargo,
                                                 String statusEleitoral) {
}
