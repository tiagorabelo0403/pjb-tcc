package com.tcc.pjb.backend.core.peticionamento.saga.domain;

import java.time.Instant;

public record ProtocoloSagaPeticionamentoResult(
        Long processoId,
        String numeroProtocolo,
        Instant dataProtocolo,
        String referenciaConector
) {
    public String numeroProcesso() { return numeroProtocolo; }
}
