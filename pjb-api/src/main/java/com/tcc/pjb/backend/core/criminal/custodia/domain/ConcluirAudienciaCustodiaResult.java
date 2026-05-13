package com.tcc.pjb.backend.core.criminal.custodia.domain;

public record ConcluirAudienciaCustodiaResult(Long custodiaId,
                                              String statusProcesso,
                                              boolean mandadoAtivo,
                                              String numeroMandado) {
}
