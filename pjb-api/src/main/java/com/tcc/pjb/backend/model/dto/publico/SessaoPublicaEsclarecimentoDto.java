package com.tcc.pjb.backend.model.dto.publico;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Instant;

public record SessaoPublicaEsclarecimentoDto(
        @JsonIgnore Long id,
        String resumoDuvida,
        String respostaPublica,
        String status,
        Instant createdAt,
        Instant respondidoEm
) {
}
