package com.tcc.pjb.backend.model.dto.publico;

import java.time.Instant;

public record SessaoPublicaEsclarecimentoDto(
        Long id,
        String resumoDuvida,
        String respostaPublica,
        String status,
        Instant createdAt,
        Instant respondidoEm
) {
}
