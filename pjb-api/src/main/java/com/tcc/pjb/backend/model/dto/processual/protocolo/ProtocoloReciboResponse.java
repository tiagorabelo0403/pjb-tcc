package com.tcc.pjb.backend.model.dto.processual.protocolo;

import java.time.Instant;

public record ProtocoloReciboResponse(
        String documentoId,
        Long processoId,
        String numero,
        String referencia,
        String sha256,
        Instant criadoEm,
        String conteudo
) {}
