package com.tcc.pjb.backend.core.identidade.grafo.domain;

import java.time.Instant;
import java.util.List;

public record IdentidadeJuridicaGraphAtualizadoEvent(
        String correlacaoId,
        String fingerprint,
        List<String> achados,
        Instant ocorridoEm
) {
    public IdentidadeJuridicaGraphAtualizadoEvent {
        achados = achados == null ? List.of() : List.copyOf(achados);
        ocorridoEm = ocorridoEm == null ? Instant.now() : ocorridoEm;
    }
}
