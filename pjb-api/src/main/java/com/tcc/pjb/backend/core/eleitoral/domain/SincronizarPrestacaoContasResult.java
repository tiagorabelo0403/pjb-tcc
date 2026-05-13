package com.tcc.pjb.backend.core.eleitoral.domain;

public record SincronizarPrestacaoContasResult(Long processoId,
                                               String status,
                                               String protocoloExterno) {
}
