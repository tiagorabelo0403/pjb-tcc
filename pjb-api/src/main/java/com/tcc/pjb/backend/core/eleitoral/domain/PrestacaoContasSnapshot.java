package com.tcc.pjb.backend.core.eleitoral.domain;

public record PrestacaoContasSnapshot(Long processoId,
                                      String numeroCandidato,
                                      String anoEleitoral,
                                      String status,
                                      String protocoloExterno,
                                      String observacao) {
}
